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
    @high_card = -1
    @round1winner = -1
    @round8winner = -1
    @QoC_winner = -1
    @QoC_played = false
  end

  def update (player_index, card)
    @cards.push(card)
    new_high_card = false
    new_low_card = false
    this_suit = int_to_suit(card/13)
    rank = card%13
    if rank == 0
      rank = 13
    end
    print 'player ', player_index.to_s, ' played ', rank.to_s, ' of ', this_suit.to_s, "\nhigh card: ", (@high_card%13), ' of ', int_to_suit(@high_card/13), "\n"
    high_card_rank = @high_card > -1? (@high_card%13 == 0? 13 : @high_card%13) : -1 #idk if i've ever written a less readable line
    if this_suit == @hand.suit
      if rank > high_card_rank
        @high_card = card
        @loser = player_index
      end
    else  #offsuit card.. what to do?
      if rank >= high_card_rank #for now we'll just make it the high card even if it's equal to current high card.
        @high_card = card
        @loser = player_index
      end
    end
    if new_high_card == true
      @loser = player_index
      print 'new high card from player ', player_index, "\n"
    end
    if new_low_card == true
      print 'new low card from player ', player_index, "\n"
    end
  end

  def new_trick
    @hand_count = -1
    @dealer = @dealer > 3 ? @dealer+1 : 0
    new_hand
  end

  def new_hand
    if @hand.penalty_value > 0
      if @hand_count == 1
        @round1winner = @loser
      end
      if @hand_count == 7
        @round1winner = @loser
      end
      if @QoC_played == true
        @QoC_winner = @loser
      end

    end
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
    @high_card = -1
    @round1winner = -1
    @round8winner = -1
    @QoC_winner = -1
    @QoC_played = false
  end

  def slobberhannes_check
    if @QoC_winner == @round1winner and @round1winner == @round8winner
      return @QoC_winner
    end
    return -1
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