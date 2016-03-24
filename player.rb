require 'socket'

class Player

  def initialize
    @active = false
    @flagged_cards = Array.new
    @name = ''
    @raw_hand = Array.new
    @position = -1
    @score = 0
  end

  def score=(value)
    @score = value
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

  def flag_cards(cardID)
    suit = cardID/13
    @raw_hand.each do |c|
      s = c/13
      if s == suit and @flagged_cards.include?(c) == false
        @flagged_cards.push(c)
      end
    end
  end

  def flagged_cards
    @flagged_cards
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

  def new_trick
    @flagged_cards.clear
  end
end