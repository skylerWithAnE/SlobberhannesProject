require 'socket'
require_relative 'util'

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
    @suit = :none
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
        #puts data, @position, @status
        #if data[0] == @position
        #  puts 'well, my turns over.'
        #  @status = waiting
        #end
        if @suit == :none
          s = data[1]/13
          case s
            when 0
              @suit = :diamonds
            when 1
              @suit = :hearts
            when 2
              @suit = :spades
            when 3
              @suit = :clubs
          end
        end
      end

      if id == '8'
        data = msg.split(',').map(&:to_i)
        @score += data[1]
        @suit = :none
        @hand.clear
      end

      if id == '9'
        @suit = :none
        @hand.clear
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

  def get_best_card
    s = suit_to_int(@suit)
    candidates = Array.new

    case s

      when -1
        @hand.each do |c|
          candidates.push(c)
        end
      when 0..3
        @hand.each do |c|
          if c/13 == s
            candidates.push(c)
          end
        end
    end
    best = -1
    candidates.each do |c|
      print "c%13 = ", c%13, " best = ", best, "\n"
      if c%13 < best
        best = c
      end
    end
    print 'best card in hand is ', best%13, ' of ', int_to_suit(best/13), ' value: ', best,   "\n"
    return best
  end

  def play_card
    best_card = get_best_card > 0 ? get_best_card : @hand[@hand.length]
    msg = '7' + @position.to_s + ',' + best_card.to_s
    @hand = @hand - [best_card]
    @socket.puts(msg)
    @status = :waiting
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
    #if turn_started
      #c.run

    #else
      #turn_started = true
      puts "hey! it's my turn!!"
      c.play_card

    #end
  end

end

if c.status == :disconnected
  puts 'disconnected, closing down.'
end

