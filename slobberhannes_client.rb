require 'socket'
class SlobberhannesClient
  def initialize(name)
    @name = name
    print 'creating client with name '
    puts @name
    @message = String.new
    @status = :waiting
  end

  def status
    @status
  end

  def connect(port)
    @socket=Socket.new(Socket::AF_INET, Socket::SOCK_STREAM, 0)
    @socket.connect( Addrinfo.tcp('127.0.0.1', port))
    print("sending name to server...\n")
    @socket.puts(@name)
    print("name sent.\n")
  end

  def disconnect
    @status = :disconnected
  end

  def run
    byte = @socket.recvfrom(1)[0]
    @message << byte
    if byte == "\n"
      id = @message[0]
      msg = @message[1..@message.length-1]
      if msg == 'startgame'
        @status = :ready
      else
        puts id, msg
      end

      @message.clear
    end

  end

  def receive_hand
    card = @socket.recvfrom(4)
    print(@name, ' got card ', card,"\n")
  end

end

c = SlobberhannesClient.new('Bill Murray')
c.connect(6666)
sleep(0.001)
while c.status == :waiting
  c.run
  sleep(0.001)
end

while c.status == :ready
  c.receive_hand
end

if c.status == :disconnected
  puts 'disconnected, closing down.'
end

