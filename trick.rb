class Hand
  def initialize
    @suit = :none
    @penalty_value = 0
  end

  def cards
    @cards
  end

  def penalty_value=(v)
    @penalty_value = v
  end

  def penalty_value
    @penalty_value
  end

  def suit=(card)
    s = card/13
    case s
      when -1
        @suit = :none
      when 0
        @suit = :diamonds
      when 1
        @suit = :hearts
      when 2
        @suit = :spades
      when 3
        @suit = :clubs
    end
    print 'Set suit of this trick to ', @suit, "\n"

  end

  def suit
    @suit
  end

end

class Trick
  def initialize
    @dealer = 0
    @loser = -1
    @hand = Hand.new
    @hand_count = 0
    @cards = Array.new
  end

  def update (player_index, card)
    new_high_card = false
    @cards.each do |c|
      if card > c
        new_high_card = true
      end
    end
    if new_high_card == true
      @loser = player_index
      print 'new high card from player ', player_index, "\n"
    end
    @cards.push(card)
  end

  def new_trick
    @hand_count += 1
    @dealer += 1
    new_hand
  end

  def new_hand
    @hand = Hand.new
    if @hand_count == 0 or @hand_count == 7
      @hand.penalty_value = 1
    end
    @dealer = @hand_count%4
    @hand_count += 1
    @hand.suit = -1
    @hand.penalty_value = 0
  end

  def hand
    @hand
  end

  def hand_count
    @hand_count
  end

  def loser
    @loser
  end

  def validate_suit (value)

  end

end