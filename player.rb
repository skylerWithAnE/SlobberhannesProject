require 'socket'

class Player

  def initialize
    @active = false
    @name = ''
    @raw_hand = Array.new
    @position = -1
    @score = 0
  end

  def deal_card(i)
    @raw_hand.push(i)
  end

  def join(s, name, position)
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

  def hand
    @raw_hand
  end

  def hand_msg
    msg = ''
    @raw_hand.each do |c|
      msg << c.to_s
      msg << ','
    end
    msg[0...msg.length-1]

  end
end