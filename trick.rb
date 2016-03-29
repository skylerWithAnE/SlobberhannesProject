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
    @last_winner = 0
    @current_winner = -1
    @hand = Hand.new
    @hand_count = 0
    @cards = Array.new
    @penalty_value = 0
    @low_card = 1000
  end

  def update (player_index, card)
    new_high_card = false
    new_low_card = false
    @cards.each do |c|
      if card%13 > c%13
        new_high_card = true
      end
      if card%13 == 0
        #ace failsafe
        new_high_card = true
      end
      if @low_card == -1
        @low_card = card
        new_low_card = true
      else
        if card%13 < @low_card%13 and card%13 > 0
          new_low_card = true
          @low_card = card
        end
      end
    end
    if new_high_card == true
      @current_winner = player_index
      print 'new high card from player ', player_index, "\n"
    end
    if new_low_card == true
      @loser = player_index
      print 'new low card from player ', player_index, "\n"
    end
    @cards.push(card)
  end

  def new_trick
    @hand_count = -1
    @dealer = @dealer > 3 ? @dealer+1 : 0
    new_hand
  end

  def new_hand
    @hand = Hand.new
    if @hand_count == 7
      @hand.penalty_value = @hand.penalty_value + 1
      print 'first or final hand.. penalty: ', @hand.penalty_value, "\n"
    end
    @hand_count += 1
    @hand.suit = -1
    @hand.penalty_value = 0
    @penalty_value = 0
    @last_winner = @current_winner
    @current_winner = -1
    @last_loser = @loser
    @loser = -1
    @low_card = -1
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

  def last_loser
    @last_loser
  end

  def dealer
    @dealer
  end

  def last_winner
    @last_winner
  end

  def winner
    @current_winner
  end

  def penalty_value
    @penalty_value
  end

  def add_penalty
    @penalty_value += 1
  end

  def validate_suit (value)

  end

end