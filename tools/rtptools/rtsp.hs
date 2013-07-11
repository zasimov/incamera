module Main where


import Network.Socket
import System.IO
import Control.Concurrent
import Control.Monad

import Data.String.Utils (splitWs, strip)


serverSocket port = do
  -- create socket
  sock <- socket AF_INET Stream 6
  -- make socket immediately reusable - eases debugging.
  setSocketOption sock ReuseAddr 1
  -- listen on TCP port 4242
  bindSocket sock (SockAddrInet port iNADDR_ANY)
  -- allow a maximum of 2 outstanding connections
  listen sock 100
  return sock
  
  
serverLoop sock = do
  -- accept one connection and handle it
  conn <- accept sock
  forkIO $ handle conn
  serverLoop sock
  
  
parseRequestString request = 
  case splitWs request of
    [method, url, _] -> (method, url)
    _ -> error $ "bad request " ++ request
    
  
splitBy1 :: Char -> String -> (String, String)
splitBy1 c s = splitBy' "" c s
  where splitBy' n c s@"" = (n, s)
        splitBy' n c (h:t) = 
          if h == c then (strip n, strip t) else splitBy' (n ++ [h]) c t
                                                 

readHeaders :: Socket -> IO [(String, String)]
readHeaders sock = readHeaders' [] sock
  where readHeaders' headers sock = do
          line <- getLineFromSocket sock >>= return . clean
          print $ "line " ++ line
          case line of
            "" -> return headers
            line -> do let header = splitBy1 ':' line
                       readHeaders' (header : headers) sock
                       
        clean :: String -> String
        clean x@"" = x
        clean s = 
          if last s == '\r'
               then take (length s - 1) s
               else s
                    
getLineFromSocket :: Socket -> IO String
getLineFromSocket socket = getLineFromSocket' "" socket
  where getLineFromSocket' s socket = do
          next <- recv socket 1
          case next of
            "" -> return s
            "\n" -> return s
            other -> getLineFromSocket' (s ++ other) socket
            
sendAll socket s = do
  n <- send socket s
  if length s == n
       then return ()
       else sendAll socket (drop n s)
                
handle :: (Socket, SockAddr) -> IO ()
handle soa@(sock, _) = do
  putStrLn ""
  putStrLn " *** NEW REQUEST *** "
  putStrLn ""
  --hdl <- socketToHandle sock ReadWriteMode
  --hSetBuffering hdl NoBuffering
  --b <- hIsEOF hdl
  request <- getLineFromSocket sock
  let (method, url) = parseRequestString request
  print method
  print url
  headers <- readHeaders sock
  print headers
  doIt sock method url headers
  handle soa
  
sendHeader sock name value = do
  putStrLn ("Send header " ++ name ++ " " ++ value)
  sendAll sock $ name ++ ": " ++ value ++ "\r\n"
  
sendContent sock content = do
  sendHeader sock "Content-length" (show (length content))
  sendAll sock "\r\n"
  putStrLn $ "Send content " ++ content
  if (length content /= 0)
      then sendAll sock content
      else return ()
     
  
doIt sock method url headers = do
  let (Just cseq) = lookup "CSeq" headers
  sendOk sock
  sendCSeq sock cseq
  sendHeader sock "Server" "VLC/2.0.5"
  case method of
    "OPTIONS" -> doOptions sock url headers
    "DESCRIBE" -> doDescribe sock url headers
    "SETUP" -> doSetup sock url headers
    "PLAY" -> doPlay sock url headers
    "TEARDOWN" -> doTeardown sock url headers
  where sendOk sock = sendAll sock "RTSP/1.0 200 OK\r\n"
        sendCSeq sock cseq = sendAll sock $ "Cseq: " ++ cseq ++ "\r\n"
    
doOptions hdl url headers = do
  sendHeader hdl "Public" "DESCRIBE,SETUP,TEARDOWN,PLAY,PAUSE,GET_PARAMETER"
  sendContent hdl ""
  return ()
  
sdp = unlines ["v=0",
               "o=- 15380147811062912935 15380147811062912935 IN IP4 samsung-ultrabook",
               "s=Unnamed",
               "i=N/A",
               "c=IN IP4 127.0.0.1",
               "t=0 0",
               "a=tool:vlc 2.0.5",
               "a=recvonly",
               "a=type:broadcast",
               "a=charset:UTF-8",
               "a=control:rtsp://127.0.0.1:8080/test.sdp",
               "m=video 5554 RTP/AVP 96",
               "b=RR:0",
               "a=rtpmap:96 H263-1998/90000",
               "a=fmtp:96 packetization-mode=1;profile-level-id=4d401e;sprop-parameter-sets=J01AHqkYGwe83gDUBAQG2wrXvfAQ,KN4JyA==;",
               "a=control:rtsp://127.0.0.1:8080/test.sdp/trackID=0",
               "m=audio 5556 RTP/AVP 96",
               "b=RR:0",
               "a=rtpmap:96 mpeg4-generic/32000",
               "a=fmtp:96 streamtype=5; profile-level-id=15; mode=AAC-hbr; config=1288; SizeLength=13; IndexLength=3; IndexDeltaLength=3; Profile=1;",
               "a=control:rtsp://127.0.0.1:8080/test.sdp/trackID=1"]
      
sdp_video = unlines ["v=0",
                     "o=- 15380147811062912935 15380147811062912935 IN IP4 samsung-ultrabook",
                     "s=Unnamed",
                     "i=N/A",
                     "c=IN IP4 127.0.0.1",
                     "t=0 0",
                     "a=tool:vlc 2.0.5",
                     "a=recvonly",
                     "a=type:broadcast",
                     "a=charset:UTF-8",
                     "a=control:rtsp://127.0.0.1:8080/test.sdp",
                     "m=video 5554 RTP/AVP 96",
                     "b=RR:0",
                     "a=rtpmap:96 H263-1998/90000",
                     "a=fmtp:96 packetization-mode=1;profile-level-id=4d401e;sprop-parameter-sets=J01AHqkYGwe83gDUBAQG2wrXvfAQ,KN4JyA==;",
                     "a=control:rtsp://127.0.0.1:8080/test.sdp/trackID=0"]
  
doDescribe hdl url headers = do
  sendHeader hdl "Content-type" "application/sdp"
  sendHeader hdl "Content-base" url
  sendHeader hdl "Cache-Control" "no-cache"
  sendContent hdl sdp_video
  return ()
  
doSetup hdl url headers = do
  sendHeader hdl "Transport" "RTP/AVP/UDP;unicast;client_port=5556-5557;server_port=32775-32776;ssrc=911B8C8C;mode=play"
  sendHeader hdl "Session" "7236fb058b63b109;timeout=60"
  sendHeader hdl "Cache-Control" "no-cache"
  sendContent hdl ""
  return ()
  
doPlay sock url headers = do
  sendHeader sock "RTP-Info" "url=rtsp://127.0.0.1:8080/test.sdp/trackID=0;seq=52722;rtptime=470003746, url=rtsp://127.0.0.1:8080/test.sdp/trackID=1;seq=45491;rtptime=167112443"
  sendHeader sock "Range" "npt=13.622325-"
  sendHeader sock "Session" "7236fb058b63b109;timeout=60"
  sendHeader sock "Cache-Control" "no-cache"
  sendContent sock ""
  return ()
  
doTeardown hdl url headers = do
  return ()
    
  
main = do
  putStrLn "Run on localhost:8080"
  ssock <- serverSocket 8080
  serverLoop ssock
  return ()
