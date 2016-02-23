require 'socket'
require_relative 'player'

class GameServer

  def initialize

    @server = TCPServer.new('0.0.0.0', 6661)  #('0.0.0.0', 7672)
    @players = Array.new
    @threads = ThreadGroup.new
    @connections = 0
    @max_connections = 1
    @real_deck = Array.new
    @raw_cards = Array.new
    @player_turn = 0
    @last_msg_sent = ''
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

  def send_msg(ps, msg, identifier)
    if msg != @last_msg_sent
      print('sending message: ', identifier, msg, "\n")
    end
    m = msg.unpack('A*')
    ps.print(identifier)
    ps.puts(m)
    ps.flush
    sleep(0.01)
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

  def handle_turn
    player = @players[@player_turn]
    send_msg(player.socket, '', 6)
    rs, ws = IO.select([player.socket], [])
    if r = rs[0]
      ret = r.read(1)
      if ret == 'w'
        sleep(0.001)
      end
    end
  end

end