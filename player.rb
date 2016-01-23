require 'socket'

class Player

  def initialize
    @active = false
    @name = ''
    @position = -1
    @score = 0
  end

  def join(s, name, position)
    puts 'set player socket'
    @socket = s
    @name = name
    @active = true
    @position = position
  end

  def active
    @active
  end

  def name
    @name
  end

  def set_name(new_name)
    @name = new_name
  end

  def socket
    @socket
  end

  def position
    @position
  end

end