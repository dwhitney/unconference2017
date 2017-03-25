> What's the point of writing programs in the relational paradigm? First off, aesthetics dammit.
> -- David Nolan (@swannodette)

I love that quote and I like that Free provides a nice looking aesthetic.

```scala
for{
  
  user        <- userService.get(username)
  bankAccount <- bankAccountService.getAccount(user)
  points      <- rewardPointsService.getRewardPoints(user)
  coffee      <- coffeeService.buyCoffee(user, bankAccount, points)
  
} yield coffee

```
