/*
 Copyright 2015 Scott Swift - This program is distributed under the
 terms of the GNU General Public License. (Plus see the LeafChat License)
*/
package com.leafdigital.colorize;

import java.nio.charset.Charset;

import com.sun.jna.Native;
import com.leafdigital.irc.api.*;

interface RxListener
{
  public void gotRx(ColorizeMsgPump msgPump, ColorizePlugin plugin, CNS cns);
}

class Responder implements RxListener
{
  @Override
  public void gotRx(ColorizeMsgPump msgPump, ColorizePlugin plugin, CNS cns)
  {
// diagnostic...
//  msgPump.frame.setTitle(convert(msgPump.hColorize)); //c2083e ac085c

    if (plugin == null || msgPump == null || cns == null)
      return;

    // This just gets a property of ColorizePlugin...
    // The property gets set in ColorizePlugin.java when the user enters
    // a command. "this.messageDisplay = msg.getMessageDisplay();"
    // So we are really just sending the text returned by YahCoLoRiZe to
    // the window the user typed the /cx command into...
    MessageDisplay pluginMd = plugin.getMessageDisplay();
    
    if (pluginMd == null || cns.data == null)
      return;

    if (cns.commandID == ColorizePlugin.REMOTE_COMMAND_TEXT)
    {
      // cns.data will have "/msg #chan some text to send"
      // or "/echo some text to send" so if first token is /echo,
      // just send the text to the status (spare) window... otherwise
      // parse out the channel/nick and send a PRIVMSG...

      // The only way I could seem to end up with the proper encoding for both
      // the server message and the text echoed to the user's room is to use
      // BOTH of the methods below and parse them separately - NOT COOL!
      // but I am not familiar enough with LeafChat to use its built-in
      // hooks properly - a TODO for YOU! (What we need is a way to send my
      // raw "/msg #chan some text to send" as a command into the room that
      // initiated a /cx command to YahCoLoRiZe...
      
      String chanNickOut = "";
      String chanNickDisp = "";
      String sOut, sDisp;
      
      if (cns.commandData == 1) // Old versions set this to -1, now it's a "utf-8" flag!
      {
        sOut = IRCMsg.convertISO(cns.data); // works to send UTF-8 over the server      
        sDisp = Native.toString(cns.data, "UTF-8"); // works to echo UTF-8 to your LeafChat room
      }
      else // ANSI mode
      {
        sOut = Native.toString(cns.data, "ANSI");      
        sDisp = sOut;
      }
      
      // parse the outgoing server text
      if (sOut.length() >= 7 && sOut.toLowerCase().indexOf("/echo ") == 0)
      {
        chanNickOut = "status";
        sOut = sOut.substring(6); // string following "/echo "
      }
      else if (sOut.length() >= 8 && sOut.toLowerCase().indexOf("/msg ") == 0)
      {
        sOut = sOut.substring(5); // string following "/msg "
        
        int i = sOut.indexOf(' '); // get delimiter after channel/nick
        
        if (i > 0)
        {
          chanNickOut = sOut.substring(0,i);
          sOut = sOut.substring(i+1);
        }
      }

      // parse the text to echo locally
      if (sDisp.length() >= 7 && sDisp.toLowerCase().indexOf("/echo ") == 0)
      {
        chanNickDisp = "status";
        sDisp = sDisp.substring(6); // string following "/echo "
      }
      else if (sDisp.length() >= 8 && sDisp.toLowerCase().indexOf("/msg ") == 0)
      {
        sDisp = sDisp.substring(5); // string following "/msg "
        
        int i = sDisp.indexOf(' '); // get delimiter after channel/nick
        
        if (i > 0)
        {
          chanNickDisp = sDisp.substring(0,i);
          sDisp = sDisp.substring(i+1);
        }
      }

      if (chanNickOut == "status")
        // Echo text to local chat window
        pluginMd.showOwnText(MessageDisplay.TYPE_MSG, "", sDisp);
      else if (!chanNickOut.isEmpty())
      {
        // This just gets a property of ColorizePlugin...
        // The property gets set in ColorizePlugin.java when the user enters
        // a command. "this.server = msg.getServer();"
        Server s = plugin.getServer();

        if (s != null)
        {
          // Send text to remote IRC Server
          s.sendLine(IRCMsg.constructBytes("PRIVMSG " + chanNickOut + " :" + sOut));
          
          // Echo text to local chat window
          pluginMd.showOwnText(MessageDisplay.TYPE_MSG, chanNickDisp, sDisp);
        }
      }
    }
  }
  
//  public static String convert(Pointer p) {
//    String s = String.format("here: %08x", p.hashCode());
//    return s;
//  }
}
