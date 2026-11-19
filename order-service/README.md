### Setting up in local
this service requires connection to oracle DB to run,
run the command 

```
ssh -N -L 29092:10.0.1.191:29092 -L 1521:10.0.1.191:1521 studentNN@3.109.249.106
```

replacing `NN` with your student number and login using your password

then run either command
`mvn spring-boot:run`
withing dir 
`/order-service/`

to run directly or
`docker compose up`
within project root
