/*
 Copyright 2015 Scott Swift - This program is distributed under the
 terms of the GNU General Public License. (Plus see the LeafChat License)
*/
package com.leafdigital.colorize;

import com.sun.jna.Native;

/**
 * Utility class to send/receive messages with YahCoLoRiZe:
 * 
 *   // Struct to xfer text between clients and YahCoLoRiZe
 *   //
 *   // DONT'T CHANGE THIS!!!! The LeafCHat, IceChat and HydraIRC plugins
 *   // all depend on this never changing!!!!! (2596 bytes)
 *   struct COLORIZENET_STRUCT
 *   {
 *     __int64 lspare;
 *     __int32 spare;
 *     __int32 commandID;
 *     __int32 commandData;
 *     __int32 serverID;
 *     __int32 channelID;
 *     __int32 lenChanNick; // length of channel or nickname string in characters, without the null
 *     __int32 lenData; // length of data string in characters, without the null
 *     BYTE chanNick[CNS_CHANNICKLEN];
 *     BYTE data[CNS_DATALEN];
 *   
 *     COLORIZENET_STRUCT()
 *     {
 *       lspare = 0;
 *       clientID = -1; // LeafChat is 5
 *       commandID = -1;
 *       commandData = -1;
 *       serverID = -1;
 *       lenChanNick = 0;
 *       lenData = 0;
 *       chanNick[0] = 0;
 *       data[0] = 0;
 *     }
 *   };
 */
//@SuppressWarnings("restriction")
// NOTE: This class will be marshaled out to a YahCoLoRiZe CNS struct
// in ColorizePlugin.java's "send" function later! We use the Memory class.
public class CNS
{
  public long lspare;
  public int clientID;
  public int commandID;
  public int commandData;
  public int serverID;
  public int channelID;
  // length of channel or nickname string in characters, without the null
  public int lenChanNick;
  //length of data string in characters, without the null
  public int lenData;
  public byte[] chanNick = new byte[ColorizePlugin.CNS_CHANNICKLEN];
  public byte[] data = new byte[ColorizePlugin.CNS_DATALEN];
  
  public CNS()
  {
    this(-1, "", "");
  }
  
  public CNS(int mode, String sChanNick, String sData)
  {
    lspare = 0;
    clientID = -1;
    commandID = mode;
    commandData = -1;
    serverID = -1;
    
    // Convert native unicode strings to utf-8 byte-arrays
    // (hopefully this is smart enough not to overflow the fixed-array lengths...!)
    chanNick = Native.toByteArray(sChanNick, "UTF-8");
    data = Native.toByteArray(sData, "UTF-8");

    // NOTE: The lengths need to be the length of the utf-8 encoded text in bytes,
    // not the length in chars! Also, you MUST subtract the null terminator length (1)
    // or YahCoLoRiZe can't read it!
    lenChanNick = chanNick.length-1;
    lenData = data.length-1;
  }
}
