/*
 *
 * Copyright (c) 2020 by Brian Reynolds
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License (LGPL) as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, copies are available
 * at http://www.opensource.org.
 */
package my_custom_component;

import VASSAL.build.Buildable;
import VASSAL.build.GameModule;
import VASSAL.command.CommandEncoder;
import VASSAL.configure.ColorConfigurer;
import VASSAL.i18n.Resources;
import VASSAL.preferences.Prefs;

import java.awt.Color;
import java.awt.Font;
import java.util.Random;

/**
 * A tight mini-mod of the VASSAL 3.4 Chatter (Chat Log), to assign chat colors based on which side players are playing
 * (rather than on whether the text is from "me" or "the other player"). So for example "Axis" chat can always be grey
 * and "Allies" chat can always be blue. This would all work better if we actually passed real player info with the chat
 * commands when sending from machine to machine, but as we presently don't this will have to do!
 *
 * To install this class in your module, assuming you have left the package name "my_custom_component" and the class name
 * "MyChatter", you must do the following:
 * 1. Ensure the class builds without errors, and find the .class file MyCustomClass.class (in .../target/classes/my_custom_component)
 * 2. Browse into your .VMOD module file (rename it to .ZIP first if needed. Or get 7-Zip or another app that makes it easy)
 * 3. Make a subdirectory "my_custom_component" inside your .VMOD/.ZIP
 * 4. Put the class file (MyCustomClass.class) into that subdirectory
 * 5. Pull out a copy of the file buildFile (or buildFile.xml from 3.5 onward) from the root directory of the .VMOD/.ZIP
 * 6. Edit the buildFile (or buildFile.xml) with a text editor
 * 7. Find the line that reads: <VASSAL.build.module.Chatter/> and change it to <my_custom_component.MyChatter/>
 * 8. If you need to rename your module back from .ZIP to .VMOD you can do it now
 * 9. Run your module! It should find YOUR "chatter" (this class) instead of VASSAL's default one. Yay!
 *
 * You can (and should) of course name my_custom_component and MyChatter something else, to match your own game and needs.
 * And so just replace each reference to either of them with the right thing.
 */
public final class MyChatter extends VASSAL.build.module.Chatter implements CommandEncoder, Buildable {

  private static final String FIRST_PLAYER_CHAT_PREF        = "CustomChatColor1"; // These are just keys used to register w/ Vassal preferences
  private static final String SECOND_PLAYER_CHAT_PREF       = "CustomChatColor2"; // ... rename for your own game (so everyone using this class doesn't clash, haha)

  private static final String FIRST_PLAYER_CHAT_COLOR_NAME  = "My Game Name - Axis Player Color";    // Put your game name and side names in here
  private static final String SECOND_PLAYER_CHAT_COLOR_NAME = "My Game Name - Allied Player Color";  // ... as it will show up in preference window

  private static final String FIRST_PLAYER_SIDE_NAME        = "Axis";       // Name of the player side (from player roster) for player 1 in your game.
  private static final String SOLO_PLAYER_SIDE_NAME         = "Solitaire";  // Name of the "Solitaire" or "Solo" player side.
  //private static final String SECOND_PLAYER_SIDE_NAME     = "Allies";     // Don't actually need to use this, because anybody not matching one
                                                                              // of the first two is automatically assumed to be this. But if it wasn't
                                                                              // here to have this explanatory comment, everyone would ask me!

  private static final Color FIRST_PLAYER_COLOR  = new Color (75, 75, 75); // Color for player 1 (in this sample, grey for Axis)
  private static final Color SECOND_PLAYER_COLOR = new Color(9, 32, 229);  // Color for player 2 (in this sample, blue for Allies)

  // These hold the colors AFTER the player has potentially reconfigured them in the Preferences window
  private Color chat1, chat2;

  /**
   * Styles a chat message based on the player who sent it.
   * Overrides VASSAL's standard "my machine" / "other machine" logic with a way to assign the CP "grey" color to whoever
   * is playing CP, and the AP "blue" color to whoever is playing AP. And green to a Ref.
   */
  @Override
  protected String getChatStyle(String s) {
    String style;

    if (s.startsWith(formatChat("").trim())) { //$NON-NLS-1$
      if (GameModule.getGameModule().getProperty(VASSAL.build.module.GlobalOptions.PLAYER_SIDE).equals(FIRST_PLAYER_SIDE_NAME)) {
        style = "player1";
      } else if (GameModule.getGameModule().getProperty(VASSAL.build.module.GlobalOptions.PLAYER_SIDE).equals(SOLO_PLAYER_SIDE_NAME)) {
        style = "solo";
      } else {
        style = "player2";
      }

      if (s.contains("@p1")) {  // A way to have explicit color chat messages in narrated playbacks. By typing @p1 or @p2 or @solo at beginning of chat line
        style = "player1";
      } else if (s.contains("@p2")) {
        style = "player2";
      } else if (s.contains("@solo")) {
        style = "solo";
      }
    } else {
      if (GameModule.getGameModule().getProperty(VASSAL.build.module.GlobalOptions.PLAYER_SIDE).equals(FIRST_PLAYER_SIDE_NAME)) {
        style = "player2";
      } else if (GameModule.getGameModule().getProperty(VASSAL.build.module.GlobalOptions.PLAYER_SIDE).equals(SOLO_PLAYER_SIDE_NAME) || GameModule.getGameModule().getProperty(VASSAL.build.module.GlobalOptions.PLAYER_SIDE).equals("<observer>")) {
        style = "solo";
      } else {
        style = "player1";
      }
    }

    return style;
  }

  /**
   * Adds our two player color styles to the HTML stylesheet
   */
  @Override
  protected void makeStyleSheet(Font f) {
    // First, let VASSAL's chatter build its normal stylesheet
    super.makeStyleSheet(f);

    if (style == null) {
      return;
    }

    // Now we will add on
    addStyle(".player1", myFont, chat1, "bold", 0);
    addStyle(".player2", myFont, chat2, "bold", 0);

    addStyle(".solo",    myFont, gameMsg2, "bold", 0); // gameMsg2 is the "Game Message #2 color" from the normal Vassal chatter, and
                                                                         // here we're just riding along with that for the Solitaire color. As an "exercise"
                                                                         // you could try adding a third configurable color and hooking it all in.
  }

  /**
   * Add two extra color preferences, one for each player side
   */
  @Override
  public void addTo(Buildable b) {
    super.addTo(b); // Let VASSAL's chatter do its normal thing

    final Prefs globalPrefs = Prefs.getGlobalPrefs(); // Get the global preferences from Vassal

    // Color preference for "Player 1"
    final ColorConfigurer otherChatColor = new ColorConfigurer(
      FIRST_PLAYER_CHAT_PREF,
      FIRST_PLAYER_CHAT_COLOR_NAME,
      FIRST_PLAYER_COLOR);

    otherChatColor.addPropertyChangeListener(e -> {
      chat1 = (Color) e.getNewValue();
      makeStyleSheet(null);
    });

    globalPrefs.addOption(Resources.getString("Chatter.chat_window"), otherChatColor);
    chat1 = (Color) globalPrefs.getValue(FIRST_PLAYER_CHAT_PREF);

    // Color preference for "Player 2"
    final ColorConfigurer myChatColor = new ColorConfigurer(
      SECOND_PLAYER_CHAT_PREF,
      SECOND_PLAYER_CHAT_COLOR_NAME,
      SECOND_PLAYER_COLOR);

    myChatColor.addPropertyChangeListener(e -> {
      chat2 = (Color) e.getNewValue();
      makeStyleSheet(null);
    });

    globalPrefs.addOption(Resources.getString("Chatter.chat_window"), myChatColor);

    chat2 = (Color) globalPrefs.getValue(SECOND_PLAYER_CHAT_PREF);

    // Now make our stylesheet
    makeStyleSheet(null);
  }


  /**
   * ANOTHER LITTLE BONUS PROJECT!!!
   *
   * A hook for inserting a console class that accepts commands - you can optionally make "commands" you can type in chat
   * @param s            - chat message
   * @param style        - current style name (contains information that might be useful)
   * @param html_allowed - flag if html_processing is enabled for this message (allows console to apply security considerations)
   * @return true        - if was accepted as a console command
   */
  @Override
  public boolean consoleHook(String s, String style, boolean html_allowed) {

    // Sample console command to roll a d6 when /d6 is typed as a chat line.
    if (s.toLowerCase().startsWith("/d6")) {
      Random ran = GameModule.getGameModule().getRNG();
      int d6 = ran.nextInt(6);
      send("* D6 = " + d6); // Messages starting with "*" display in game message font. Messages starting with "-" display as a system message.
    }

    return false;
  }
}
