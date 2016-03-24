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

def suit_to_int(suit)
  value = -1
  case suit
    when :diamonds
      value = 0
    when :hearts
      value = 1
    when :spades
      value = 2
    when :clubs
      value = 3
  end
  return value
end