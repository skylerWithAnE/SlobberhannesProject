require 'socket'
require_relative 'player'

class GameServer

  def initialize

    @dts = TCPServer.new('0.0.0.0', 7672)

    @players = Array.new
    @threads = ThreadGroup.new
    @connections = 0
    @max_connections = 4
    for i in 0...@max_connections do
      @players.push(Player.new)
    end
    @status = :initialized
    puts 'Server initialized...'
  end

  def status
    @status
  end

  def send_msg(ps, msg)
    m = msg.unpack('A*')
    ps.print(m)
    sleep(0.01)
  end

  def get_players
    @threads.add(Thread.start(@dts.accept) do |s|
      puts 'new thread started'
      name = s.gets.chomp
      @players[@connections].join(s, name, @connections)
      p = @players[@connections]
      greeting = 'Welcome to the server, ' + name + '!'
      send_msg(p.socket, greeting)
      @connections += 1
    end)
  end

  def new_player_notify
    for i in 0...@connections-1
      notification = ('New player ' + @players[@connections-1].name + ' has joined the server!')
      send_msg(@players[i].socket, notification)
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
    if @connections == @max_connections
      puts 'good news! server is full!'
      @status == :full
      start_game
    end
  end

  def start_game
    puts 'Max reached!'
    @players.each do |p|
      print('Sending message to ',p.name,"\n")
      send_msg(p.socket, 'startgame')
    end
  end

  def shut_down
    @players.each do |p|
      send_msg(p.socket, 'shutdown')
      sleep 0.1
      p.socket.close
    end
    @threads.each do |thr|
      thr.exit
    end
  end

end