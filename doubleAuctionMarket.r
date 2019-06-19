# Read the market CSV

market <- read.csv(file="market.csv", header=TRUE, sep=",")

# Plot the price of the share

plot(market$Step,market$Price,type="l",main="Double Auction Market",xlab="Step",ylab="Price")

# Read the traders CSV

traders <- read.csv(file="traders.csv", header=TRUE, sep=",")

# Wealth Distribution

hist(traders$Wealth,main="Wealth Distribution")
