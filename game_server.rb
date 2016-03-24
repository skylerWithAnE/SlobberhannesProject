require 'socket'
require_relative 'player'
require_relative 'trick'

class GameServer

  def initialize

    @server = TCPServer.new('0.0.0.0', 6661)  #('0.0.0.0', 7672)
    @players = Array.new
    #@threads = ThreadGroup.new
    @connections = 0
    @max_connections = 4
    @real_deck = Array.new
    @raw_cards = Array.new
    @trick = Trick.new
    @player_turn = 0
    @last_msg_sent = ''
    @waiting_for_player = false
    @player_turn_msg = ''
    for c in 0...52 do
      @raw_cards.push(c)
    end
    for i in 0...@max_connections do
      @players.push(Player.new)
    end
    @status = :initialized
    puts 'Server initialized...'
  end

  def status
    @status
  end

  def new_trick
    @trick = Trick.new
  end

  def send_msg(ps, msg, identifier)
    if msg != @last_msg_sent
      print('sending message: ', identifier, msg, "\n")
    end
    m = msg.unpack('A*')
    ps.print(identifier, msg, "\n")
    #ps.puts(m)
    ps.flush
    sleep(0.1)
    @last_msg_sent = msg
  end

  def get_players
    #@threads.add((Thread.start(@s.accept) do |s|
      s = @server.accept()
      puts 'new thread started'
      name = s.gets.chomp
      @players[@connections].join(s, name, @connections)
      p = @players[@connections]
      puts 'trying to send a message'
      greeting = @connections.to_s + ',Welcome to the server ' + name + '!'
      puts greeting
      send_msg(p.socket, greeting, 0)
      @connections += 1
    #end).join)
  end

  def new_player_notify
    for i in 0...@connections-1
      notification = @players[@connections-1].position.to_s + ',' + @players[@connections-1].name
      puts notification
      send_msg(@players[i].socket, notification, 2)
      send_msg(@players[@connections-1].socket, @players[i].position.to_s + ',' + @players[i].name, 2)
    end
  end

  def poll_clients
    @players.each do |p|

    end
  end

  def update
    print('Players: ')
    puts @connections
    @players.each do |p|
      if p.name != ''
        print('socket: ', p.socket, ' name: ')
        puts p.name
      end
    end
    sleep(1)
    new_player_notify
    sleep(0.1)
  #rescue Errno::EPIPE
    #puts 'some client disconnected... No support for this right now, sorry!'
    if @connections == @max_connections
      puts 'good news! server is full!'
      @status == :full
      start_game
    end
  end

  def deal_cards
    illegal_cards = [ 0, 1, 2, 3, 4,
                     14,15,16,17,18,
                     27,28,29,30,31,
                     40,41,42,43,44]
    illegal_cards.each do |c|
      @raw_cards.delete(c);
    end
    @raw_cards = @raw_cards.shuffle
    for i in 0...8 do
      @players.each do |p|
        p.deal_card(@raw_cards.pop)
      end
    end
  end

    #q = rc/13  #number of cards per suit.
    #r = rc%13  #rank of the card.

  def start_game
    puts 'Max reached!'
    deal_cards()
    @players.each do |p|
      print('Sending message to ', p.name, "\n")
      send_msg(p.socket, p.hand_msg, 5)
    end
    @status = :playing
  end

  def int_to_suit(cardID)
    suit = :none
    case cardID
      when 0
        suit = :diamonds
      when 1
        suit = :hearts
      when 2
        suit = :spades
      when 3
        suit = :clubs
    end
    return suit
  end

  def validate_suit(cardID, player)
    s = cardID/13
    suit = 0
    case @trick.hand.suit
      when :diamonds
        suit = 0
      when :hearts
        suit = 1
      when :spades
        suit = 2
      when :clubs
        suit = 3
    end
    trick_suit = @trick.hand.suit
    player.hand.each do |c|

    end
  end

  def handle_turn
    #puts 'handle turn loop...'
    player = @players[@player_turn]
    if not @waiting_for_player
      send_msg(player.socket, player.position.to_s, 6)
      @waiting_for_player = true
    end
    rs, ws = IO.select([player.socket], [])
    if r = rs[0]
      ret = r.read(1)
      if ret != "\n"
        @player_turn_msg << ret
      else
        @player_turn_msg = @player_turn_msg[1...@player_turn_msg.length]
        data = @player_turn_msg.split(',').map(&:to_i)
        if data[0] == @player_turn
          card_index = player.hand.index(data[1])
          if card_index == nil
            print "Player tried to play a card that doesn't exist in their hand. Card: " ,data[1],"\n"
          else
            puts 'A valid card has been played.'
            if data[1] == 50   #queen of clubs
              @trick.hand.penalty_value = @trick.hand.penalty_value + 1
              puts 'Queen of Clubs played this trick.'
            end
            if @player_turn == 0  #set suit of the first card played this trick.
              @trick.hand.suit = data[1]
            end
            if player.flagged_cards.include?(data[1])
              puts 'We got a cheater over here.'
            end
            if @trick.hand.suit != int_to_suit(data[1])
              player.flag_cards(data[1])
              if player.flagged_cards.size > 0
                puts player.flagged_cards, data[1]
              end
            end
            player.hand.delete_at(card_index)
            out_msg = data[0].to_s + ',' + data[1].to_s
            @players.each do |p|
              send_msg(p.socket, out_msg, 7)
            end
            #increment player turn and keep going.
            @player_turn += 1
            @player_turn_msg.clear
            @waiting_for_player = false
            if @player_turn >= @max_connections
              puts 'trick finished...'
              @player_turn = 0
              @trick.new_hand
            end
            if @trick.hand_count == 8
              #end of trick, send out point values.
              puts 'time for a new trick.'
              while 1
                x = 1+1
              end
            end
          end
        else
          puts 'Wrong player tried to take make a move.'
        end

      end
    end

  end

end

=begin
  problem sending cards to java client, sometimes causes index-out-of-range exception.
  messages to java client are frequently being misinterpreted, especially when messages come in quick succession.
  583
=end
