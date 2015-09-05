/*
 Copyright 2015 Scott Swift - This program is distributed under the
 terms of the GNU General Public License. (Plus see the LeafChat License)
*/
package com.leafdigital.colorize;

import javax.swing.JOptionPane; // Useful for debug! (showMessageDialog())

import leafchat.core.api.*;

import com.leafdigital.irc.api.*;
// Java Native Access (JNA)
import com.sun.jna.*;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.ptr.IntByReference;
//import com.sun.jna.ptr.PointerByReference; // only needed for ChangeWindowMessageFilterEx()

public class ColorizePlugin implements Plugin
{
  public int RWM_ColorizeNet = 0; // Custom Windows Message ID...

  public static final String YC_VERSION = "1.6"; // we send this to YahCoLoRiZe
  
  // This is only applicable for data sent TO YahCoLoRiZe not
  // received FROM YahCoLoRiZe!
  public static final int LEAFCHAT_ID = 5; // clientID YahCoLoRiZe expects (do not change from 5!)
  
  // Commands we accept from the input window...
  // (can be changed to anything you want but must be lower-case!)
  public static final String C_PLAY = "cplay"; // Controls text-playback from YahCoLoRiZe
	public static final String C_CHAN = "cchan"; // Remotely change the channel (room) text is sent to
	public static final String C_TIME = "ctime"; // Remotely change text playback speed (in milliseconds)
	public static final String C_SEND = "cx"; // Send a line of text to YahCoLoRiZe for processing
	public static final String C_HELP = "chelp"; // Echo a list of commands

  // C_PLAY argument strings
	public static final String A_START = "start"; // Starts/restarts text-playback
	public static final String A_STOP = "stop"; // Stops text-playback
	public static final String A_PAUSE = "pause"; // Pauses text-playback
	public static final String A_RESUME = "resume"; // Resumes text-playback

  // Max # of bytes to send/receive in the data field of COLORIZENETSTRUCT
  // (Can't be changed!)
	public static final int CNS_DATALEN = 2048;
	
  // Max # of bytes to send/receive in the ChanNick field of COLORIZENETSTRUCT
  // (Can't be changed!)
	public static final int CNS_CHANNICKLEN = 512;
	
  // COLORIZENETSTRUCT, allocate memory of 8+(4*7)+CNS_DATALEN+CNS_CHANNICKLEN bytes
  // to marshal it for both 32 and 64 bit platforms. Add 4 for the 32-bit pointer to
	// a constructor at the end of the structure which YahCoLoRiZe has...
  public static final int CNS_SIZE = 8+(4*7)+CNS_DATALEN+CNS_CHANNICKLEN;
  
  public static final int CDS_SIZE = 12; // this is always 12 (for all platforms) because YahCoLoRiZe is 32-bit!

  // Registered with RegisterWindowsMessage()
	private static final String YC_SIGNITURE = "WM_ColorizeNet";

	// YahCoLoRiZe class and window names used to find its handle
	public static final String YC_CLASSNAME = "TDTSColor";
  public static final String YC_WINDOWNAME = "YahCoLoRiZe";
  
  // SendMessageTimeout flags
  private static final int TIMEOUT_INT = 5000; // Wait 5 seconds
  private static final int SMTO_NORMAL = 0x0000;
  //private static final int SMTO_BLOCK = 0x0001;
  //private static final int SMTO_ABORTIFHUNG = 0x0002;
  //private static final int SMTO_NOTIMEOUTIFNOTHUNG = 0x0008;
  //private static final int SMTO_ERRORONEXIT = 0x0020;

  // Remote commands
	public static final int REMOTE_COMMAND_START = 0;
	public static final int REMOTE_COMMAND_STOP = 1;
	public static final int REMOTE_COMMAND_PAUSE = 2;
	public static final int REMOTE_COMMAND_RESUME = 3;
	public static final int REMOTE_COMMAND_CHANNEL = 4;
	public static final int REMOTE_COMMAND_TIME = 5;
	public static final int REMOTE_COMMAND_ID = 6;
	public static final int REMOTE_COMMAND_FILE = 7;
	public static final int REMOTE_COMMAND_TEXT = 8;

	private ColorizeMsgPump msgPump = null;
	
  // Properties...
  private Server server = null; // server associated with message
  protected Server getServer() { return server; }

  // This gets set when we type in a command. Then we can access it globally
  // via this property...
  private MessageDisplay messageDisplay = null;
  protected MessageDisplay getMessageDisplay() { return messageDisplay; }
  
  private String channel = null;
  protected String getChannel() { return channel; }
  
  private String otherNick = null;
  protected String getOtherNick() { return otherNick; }
  
  private String ourNick = null;
  protected String getOurNick() { return ourNick; }
  
  /**
   * JNA interface with Window's user32.dll
   */
  public interface User32 extends StdCallLibrary {
    
     User32 INSTANCE = (User32)Native.loadLibrary("user32",
         User32.class, W32APIOptions.UNICODE_OPTIONS);

     // http://stackoverflow.com/questions/4901609/how-to-handle-wm-queryendsession-messages-with-jna      
     interface WindowProc extends StdCallCallback {
       LRESULT callback(HWND hWnd, int uMsg, HWND wParam, LPARAM lParam);
     }
     
     // various constants
     public static final int WM_COPYDATA = 0x004A;
//     public static final int WM_QUERYENDSESSION = 0x0011;
     public static final int GWL_WNDPROC = -4;
     public static final int MSGFLT_ALLOW = 1;
     
     long SendMessageTimeout(HWND hTargWnd, int msg, HWND hSourceWnd,
         byte[] data, int flags, int timeout, IntByReference ret);
     int RegisterWindowMessage(String sMsg);
     HWND FindWindow(String wClass, String wName);
     int SetWindowLongW(Pointer hWnd, int nIndex, WindowProc dwNewLong);
     LRESULT DefWindowProcW(HWND hWnd, int uMsg, HWND wParam, LPARAM lParam);

// This will throw an exception if run on Win XP as it is not in Kernal32.dll!
//     boolean ChangeWindowMessageFilterEx(HWND hWnd, int uMsg, 
//         int mode, PointerByReference pChangeFilterStruct);
  }
  
  public interface Kernel32 extends StdCallLibrary {
    Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);
    void RtlMoveMemory(byte[] to, int from, int count);
    void RtlMoveMemory(Pointer to, LPARAM from, int count);
 }
  
  /**
   * Plugin's Initialization hook called from LeafChat
   */
  @Override
	public synchronized void init(PluginContext context, PluginLoadReporter reporter) throws GeneralException
	{
		context.requestMessages(UserCommandMsg.class, this);
		
		// Register unique MS Windows message to identify our communication
    RWM_ColorizeNet = User32.INSTANCE.RegisterWindowMessage(YC_SIGNITURE);
    
		msgPump = new ColorizeMsgPump(context); // Make window to handle WM_COPYDATA

		// Examples of accessing an API:
    //PreferencesUI preferencesUI = context.getSingle(PreferencesUI.class);
		//UI ui = context.getSingle(UI.class);    
    //IRCUI ircUI = context.getSingle(IRCUI.class);
    //messageDisplay = ircUI.getMessageDisplay(null);
    
    messageDisplay = null;
      
		// For Windows 7 and up we need this to unblock reception of WM_COPYDATA from
		// YahCoLoRiZe! Will throw an UnsatisfiedLinkError for XP!
		//
		// (Turns out we don't - but it might come in handy some day!)
		//
//		try {
//      if (!User32.INSTANCE.ChangeWindowMessageFilterEx(new HWND(msgPump.hComponent), User32.WM_COPYDATA, 
//          User32.MSGFLT_ALLOW, null))
//        JOptionPane.showMessageDialog(null, "YahCoLoRiZe plugin for LeafChat could not enable data-communications!",
//            "Info!", JOptionPane.WARNING_MESSAGE);
//		} catch(Exception e) {
//    }
    
    if (Platform.is64Bit())
      JOptionPane.showMessageDialog(null, "The YahCoLoRiZe plugin requires 32-bit Java!\n" +
          "Go to: http://www.java.com/en/download/faq/java_win64bit.xml\n" +
          "and click the link to install 32-bit Java on your 64-bit machine!\n\n" +
          "You will also need to create a shortcut on your desktop to launch LeafChat in 32-bit mode:\n" +
          "1. Right-click on the desktop and choose New->Shortcut\n" +
          "2. In \"Target\" put this exactly, keeping the quotes as they are:\n\n" +
          "       \"C:\\Program Files (x86)\\Java\\jre1.8.0_45\\bin\\javaw.exe\" -jar -d32 leafChat.jar\n\n" +
          "3. In \"Start In\" put this (with the quotes):\n\n" +
          "       \"C:\\Program Files (x86)\\leafChat 2\"\n\n" +
          "4. Click OK then right-click the new shortcut and Rename it to \"LeafChat\"\n" +
          "(The 1.8.0_45 will change... so check in C:\\Program Files (x86)\\Java for your version and use that.)",
          "Info", JOptionPane.WARNING_MESSAGE);
      
    // ColorizeWindow is the initiator of a WM_COPYDATA received event.
		// Here, we add a responder to take action when the event occurs.
    Responder responder = new Responder();
		msgPump.addListener(responder);
		
    // Send our version and handle to YahCoLoRiZe
    send(msgPump.hComponent, new CNS(REMOTE_COMMAND_ID, "", YC_VERSION));
	}

  /**
   * Called when the user types in a command such as "/cx text to process"
   */
	public void msg(UserCommandMsg msg)
	{
	  if (msg == null || msgPump == null) return;

    this.messageDisplay = msg.getMessageDisplay();
    this.server = msg.getServer();
    
    String cmd = msg.getCommand();
    
    // This CAN be null and throws an exception!
    if (cmd == null || cmd.isEmpty()) return;
    
    String sChanNick = "", sData = "";
    int iCommandID = -1, iCommandData = -1;
    
    if(cmd.equals(C_SEND))
		{
      try {
        updateGlobals(msg);
        
        sData = msg.getParams(); // text following the /cx
        
        iCommandID = REMOTE_COMMAND_TEXT;
        
        // The old ANSI plugin left this -1 - so we will send 1 to let
        // YahCoLoRiZe know this is UTF-8 encoding!
        iCommandData = 1;
            
        try {
          // this throws an exception if the string is null!
          sChanNick = channel;
          if (sChanNick.isEmpty())
            sChanNick = "status"; // Tell YahCoLoRiZe to send to the status window
        } catch(Exception e) {
          sChanNick = "status"; // Tell YahCoLoRiZe to send to the status window
        }
        
      } catch(Exception e) {
      }
		}
    else if(cmd.equals(C_PLAY))
    {
      try {
        String parms = msg.getParams().trim(); // text following /cplay
        if (parms == null || parms.isEmpty()) return;
        
        String mode = parms.toLowerCase();
        
        // stop start pause resume
        if (mode.equals(A_START))
          iCommandID = REMOTE_COMMAND_START;
        else if (mode.equals(A_STOP))
          iCommandID = REMOTE_COMMAND_STOP;
        else if (mode.equals(A_PAUSE))
          iCommandID = REMOTE_COMMAND_PAUSE;
        else if (mode.equals(A_RESUME))
          iCommandID = REMOTE_COMMAND_RESUME;
        else // filename
        {
          iCommandID = REMOTE_COMMAND_FILE;
          sData = parms;
        }        
      } catch (Exception e) {
      }
    }
    else if(cmd.equals(C_CHAN))
    {
      try {
        String parms = msg.getParams().trim(); // text following /cchan
        if (parms == null || parms.isEmpty()) return;
        iCommandID = REMOTE_COMMAND_CHANNEL;
        sData = parms;
      } catch (Exception e) {
      }
    }
    else if(cmd.equals(C_TIME))
    {
      try {
        String parms = msg.getParams().trim(); // text following /cchan
        if (parms == null || parms.isEmpty()) return;
        
        iCommandID = REMOTE_COMMAND_TIME;
        int time = -1;
          
        try {
          time = Integer.parseInt(parms);
        } catch (NumberFormatException e) {
        }

        if (time > 0)
          iCommandData = time;
      } catch (Exception e) {
      }
    }
    else if(cmd.equals(C_HELP))
    {
      if (this.messageDisplay != null)
      {
        messageDisplay.showInfo("*************************************************");
        messageDisplay.showInfo("* CoLoRiZe Plugin Help... www.yahcolorize.com");
        messageDisplay.showInfo("*************************************************");
        messageDisplay.showInfo("* Run YahCoLoRiZe and in the \"Client\"");
        messageDisplay.showInfo("* menu choose \"LeafChat\".");
        messageDisplay.showInfo("* Next, in LeafChat type: /cx TEST123.");
        messageDisplay.showInfo("*");
        messageDisplay.showInfo("* Text will be sent back to the room you are in.");
        messageDisplay.showInfo("*");
        messageDisplay.showInfo("* Examples:");
        messageDisplay.showInfo("* /cx 0 My Text (random text-effect)");
        messageDisplay.showInfo("* /cx 12 My Text (uses text-effect 12)");
        messageDisplay.showInfo("* /cchan #MyRoom (channel YahCoLoRiZe sends to)");
        messageDisplay.showInfo("* /ctime 3000 (sets time-per-line to 3 seconds)");
        messageDisplay.showInfo("* /cplay C:\\CHAT\\MyFile.txt (plays file)");
        messageDisplay.showInfo("* /cplay start|stop|pause|resume");
        messageDisplay.showInfo("*");
        messageDisplay.showInfo("* *NOTE: Use the \"Courier New\" font for");
        messageDisplay.showInfo("* even text-borders!");
        messageDisplay.showInfo("*************************************************");
        msg.markHandled();
      }
    }

    // Send the data if we have a command to send...
    if (iCommandID >= 0)
    {
      int iServerID = -1;
      
      try {
        if (server != null)
          iServerID = server.hashCode(); // not sure if this is of any use...
      } catch(Exception e) {
      }
      
      try {
        CNS cns = new CNS(iCommandID, sChanNick, sData);
        cns.commandData = iCommandData;
        cns.serverID = iServerID;
        send(msgPump.hComponent, cns); // send it to YahCoLoRiZe
        msg.markHandled(); // Success in handling the message...
      } catch (Exception e) {
      }
    }
  }
	
  /**
   * Update global properties after user types a message
   */
  public void updateGlobals(UserCommandMsg msg)
  {
    // save info globally
    try {
      this.messageDisplay = msg.getMessageDisplay();
    } catch (Exception e1) {
    }

    try {
      this.server = msg.getServer();
      this.ourNick = this.server.getOurNick();
    } catch (Exception e1) {
    }

    // Throws exception at times!
    try {
      this.otherNick = msg.getContextUser().getNick();
    } catch (Exception e1) {
      this.otherNick = "";
    }
    
    this.channel = msg.getContextChan();
  }
  
  @Override
	public void close() throws GeneralException	{
		if(msgPump!=null) msgPump.close();			
	}
	
	public void windowClosed() {
	  msgPump=null;
	}
	
  @Override
	public String toString() {
	  // Used to display in system log etc.
		return "YahCoLoRiZe plugin for LeafChat";
	}	
  /* 
   * Marshal the COLORIZENET and COPYDATA structs to Native Memory
   * and send to YahCoLoRiZe
   *
   *  struct COLORIZENET_STRUCT
   * {
   *   __int64 lspare;
   *   __int32 clientID; // LeafChat is 5
   *   __int32 commandID;
   *   __int32 commandData;
   *   __int32 serverID;
   *   __int32 channelID;
   *   __int32 lenChanNick; // length of channel or nickname string in characters, without the null
   *   __int32 lenData; // length of data string in characters, without the null
   *   BYTE chanNick[CNS_CHANNICKLEN];
   *   BYTE data[CNS_DATALEN];
   * }
   * 
   * struct COPYDATA
   * {
   *  int dwData; // 4 bytes for 32-bit platform, 8 bytes for 64-bit platform
   *  int cbData;
   *  int lpData;
   * }
  */
  public long send(Pointer hSourceWnd, CNS cns)
  {
    cns.clientID = LEAFCHAT_ID; // LeafChat's permanent ID in YahCoLoRiZe

    try
    {
      // Find the YahCoLoRiZe window-handle and exit if it's null
      if ( (msgPump.hColorize = User32.INSTANCE.FindWindow(YC_CLASSNAME, YC_WINDOWNAME) ) == null)
        return 0;
    }
    catch (Exception e)
    {
      return 0;
    }
      
    Memory mCds = null; // COPYDATA Structure
    Memory mCns = null; // COLORIZENET Structure
    
    try
    {
      mCns = new Memory(CNS_SIZE);
      mCds = new Memory(CDS_SIZE+4); // add 4 to permit a 64-bit pointer at the end...
      
      int idx = 0;

      // NOTE: YahCoLoRiZe is 32-bit - presently, this plugin IS NOT WORKING when LeafChat
      // runs on a 64-bit Java Virtual Machine!!!!!!!!
      if (Platform.is64Bit())
      {
        // Write COLORIZENETSTRUCT to memory
        mCns.setInt(idx, (int)cns.lspare); idx+=8;
        mCns.setShort(idx, (short)cns.clientID); idx+=4;
        mCns.setShort(idx, (short)cns.commandID); idx+=4;
        mCns.setShort(idx, (short)cns.commandData); idx+=4; 
        mCns.setShort(idx, (short)cns.serverID); idx+=4; 
        mCns.setShort(idx, (short)cns.channelID); idx+=4; 
        mCns.setShort(idx, (short)cns.lenChanNick); idx+=4;  
        mCns.setShort(idx, (short)cns.lenData); idx+=4;
        mCns.write(idx, cns.chanNick, 0, cns.chanNick.length); idx += CNS_CHANNICKLEN; // chanNick[CNS_CHANNICKLEN])
        mCns.write(idx, cns.data, 0, cns.data.length); // idx += CNS_DATALEN (data[CNS_DATALEN])
        
        // Write COPYDATASTRUCT to memory
        mCds.setShort(0, (short)RWM_ColorizeNet);
        mCds.setShort(4, (short)CNS_SIZE);
      }
      else
      {
        // Write COLORIZENETSTRUCT to memory
        mCns.setLong(idx, cns.lspare); idx+=8;
        mCns.setInt(idx, cns.clientID); idx+=4;
        mCns.setInt(idx, cns.commandID); idx+=4;
        mCns.setInt(idx, cns.commandData); idx+=4; 
        mCns.setInt(idx, cns.serverID); idx+=4; 
        mCns.setInt(idx, cns.channelID); idx+=4; 
        mCns.setInt(idx, cns.lenChanNick); idx+=4;  
        mCns.setInt(idx, cns.lenData); idx+=4;
        mCns.write(idx, cns.chanNick, 0, cns.chanNick.length); idx += CNS_CHANNICKLEN; // chanNick[CNS_CHANNICKLEN])
        mCns.write(idx, cns.data, 0, cns.data.length); // idx += CNS_DATALEN (data[CNS_DATALEN])
        
        // Write COPYDATASTRUCT to memory
        mCds.setInt(0, RWM_ColorizeNet);
        mCds.setInt(4, CNS_SIZE);
      }

      // write 4 bytes for a 32-bit JVM (or 8 for a 64-bit JVM) at offset = 8
      mCds.setPointer(8, mCns.share(0));
      
      // Send it to YahCoLoRiZe
      long ret = sendMessage(msgPump.hColorize, hSourceWnd, mCds.getByteArray(0, CDS_SIZE+4));

      return ret;
    }
    catch(Exception ex)
    {
      return 0;
    }
  }

  long sendMessage(HWND hTargWnd, Pointer hSourceWnd, byte[] data)
  {
    IntByReference ret = new IntByReference();
    return User32.INSTANCE.SendMessageTimeout(hTargWnd, User32.WM_COPYDATA,
        new HWND(hSourceWnd), data, SMTO_NORMAL, TIMEOUT_INT, ret);
  }  
}
