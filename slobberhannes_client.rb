require 'socket'
class SlobberhannesClient
  def initialize(name)
    @name = name
    print 'creating client with name '
    puts @name
    @message = String.new
    @status = :waiting
    @game_status = :not_started
    @hand = Array.new
    @position = -1
  end

  def status
    @status
  end

  def game_status
    @game_status
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
      puts id, msg


      #I should probably place this in a switch
      if id == '0'
        data = msg.split(',')
        @position = data[0]
        print 'Server says:' + data[1].chomp + ' Position: ' + @position + "\n"
      end

      if id == '5'
        @hand = msg.split(',').map(&:to_i)
        puts @hand
      end

      if id == '6'
        @status = :my_turn
      end

      if id == '7'
        data = msg.split(',').map(&:to_i)
        puts data, @position, @status
        if data[0] == @position
          puts 'well, my turns over.'
          @status = waiting
        end
      end

      @message.clear
    end

  end

  def receive_hand
    card = @socket.recvfrom(4)
    print(@name, ' got card ', card,"\n")
    @status = :waiting
    @game_status = :waiting_for_turn
  end

  def play_card
    msg = '7' + @position.to_s + ',' + @hand.pop.to_s
    @socket.puts(msg)
    #@status = :waiting
  end

end

c = SlobberhannesClient.new('Bill Murray')
c.connect(6661)
sleep(0.001)
while c.status == :waiting
  c.run
  sleep(0.001)
end

while c.status == :ready
  c.receive_hand
end

sleep(1)

turn_started = false
while c.game_status != :game_over
  while c.status == :waiting
    c.run
    sleep(0.001)
  end

  while c.status == :my_turn
    if turn_started
      c.run

    else
      turn_started = true
      puts "hey! it's my turn!!"
      c.play_card
    end
  end

end


if c.status == :disconnected
  puts 'disconnected, closing down.'
end

