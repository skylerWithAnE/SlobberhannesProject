require 'socket'
require_relative 'player'
require_relative 'trick'
require_relative 'util'

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
    @turn_count = 0
    @last_msg_sent = ''
    @waiting_for_player = false
    @cards_dealt  = false
    @player_turn_msg = ''
    for i in 0...@max_connections do
      @players.push(Player.new)
    end
    @status = :initialized
    puts 'Server initialized...'
  end

  def status
    @status
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
      puts 'new player joined'
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
    @players.each do |p|
      p.new_round()
    end
    print 'dealing cards...'
    @raw_cards = Array.new
    for c in 0...52 do
      @raw_cards.push(c)
      puts c
    end
    illegal_cards = [ 1, 2, 3, 4, 5,
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
    puts 'finished dealing cards.'
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
    @cards_dealt = true
    @status = :playing
  end

  def handle_turn
    #puts 'handle turn loop...'
    if @cards_dealt
      player = @players[@player_turn]
      if not @waiting_for_player
        if @turn_count == 0
          if @trick.hand_count == 0 or @trick.hand_count == 7
            puts 'first or final hand'
            @trick.add_penalty
          end
        end
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
                #@trick.hand.penalty_value = @trick.hand.penalty_value + 1
                @trick.add_penalty
                puts 'Queen of Clubs played this trick.'
              end
              if @turn_count == 0  #set suit of the first card played this trick.
                @trick.hand.suit = data[1]
              end
              if player.flagged_cards.include?(data[1])
                puts 'We got a cheater over here.'
                #need to send a message to all clients to clear their hands
                #@players.each do |p|
                # send_msg(p.socket, @players[turn_count], 'b')
                #end
                @player_turn_msg.clear
                @waiting_for_player = false
                player.score = player.score + 4
                @players.each do |p|
                  score_msg = p.position.to_s + ',' + p.score.to_s
                  send_msg(p.s, score_msg, 8)
                  send_msg(p.s, player.position.to_s, 9)
                  p.score_this_round = 0
                  p.hand.clear
                end
                @turn_count = 0
                start_game
                return
              end
              print 'comparing ', @trick.hand.suit, ' to ', int_to_suit(data[1]/13), "\n"
              if @trick.hand.suit != int_to_suit(data[1]/13)
                puts 'card played was offsuit, flagging cards.'
                player.flag_cards(suit_to_int(@trick.hand.suit))
              end
              player.hand.delete_at(card_index)
              out_msg = data[0].to_s + ',' + data[1].to_s
              @players.each do |p|
                send_msg(p.socket, out_msg, 7)
              end
              @trick.update(@player_turn, data[1])
              #increment player turn and keep going.
              @player_turn += 1
              @turn_count += 1
              @player_turn_msg.clear
              @waiting_for_player = false
              if @turn_count >= @max_connections
                @players[@trick.loser].score_this_round = @players[@trick.loser].score_this_round + @trick.penalty_value
                print 'loser score: ', @players[@trick.loser].score_this_round, "\n"
                @turn_count = 0
                print 'winner of the hand ', @trick.winner, "\n"
                index = 0
                @players.each do |p|
                  p.score_this_round = p.score_this_round == 3? p.score_this_round + 1 : p.score_this_round
                  score_msg = index.to_s + ',' + p.score_this_round.to_s
                  send_msg(p.socket, score_msg, 8)
                  index += 1
                end
                @players.each do |p|
                  send_msg(p.socket, @trick.loser.to_s, 9)
                end
                @player_turn = @trick.loser
                @players.each do |p|
                  if p.score >= 10
                    @status = :gameover
                    return
                  end
                end
                @trick.new_hand
              end
              if @player_turn >= @max_connections #need to make this active player count (elimination?)
                @player_turn = 0
              end
              if @trick.hand_count == 8
                puts 'time for a new trick.'
                #slobberhannes_penalty = @trick.slobberhannes_check
                #if slobberhannes_penalty >= 0
                  #@players[slobberhannes_penalty].score_this_round = @players[slobberhannes_penalty].score_this_round + 1
                #end
                @players.each do |p|
                  p.score = p.score_this_round
                  #p.score_this_round = 0
                end
                @trick.new_trick
                @player_turn = @trick.dealer
                @cards_dealt = false
                start_game
              end
            end
          else
            puts 'Wrong player tried to take make a move.'
          end

        end
      end
    end

  end

  def end_game
    winner = -1
    loser = -1
    high_score = -1
    low_score = 10

    @players.each do |p|
      if p.score < low_score
        winner = p.position
        low_score = p.score
      end
      if p.score > high_score
        loser = p.position
        high_score = p.score
      end
    end
    @players.each do |p|
      send_msg(p.socket, winner.to_s,'a')
    end
    print 'Game over! Player ', winner, ' had the lowest score.  Player ', loser, ' had the highest score', "\n"
  end

end

=begin
  end game when a player reaches 10 points

  do i have the turn order stuff correct?
    winner of the previous trick plays first card on the next trick

  not actually failing when someone reveals that they have played out of suit
    throw away current hand and redeal at current dealer, or increment dealer?

  lots of orphan code

  the hand class and trick class are really damn confusing, redo that if there's time
    is a trick the playtime between when the cards are dealt?
=end
