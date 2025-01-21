1 requirements
1.1

functional requirements
uuid
iscloseto
isinregion
nextpos
isvalidorder
pathgeneration
pathgeojson

performance attributes and requirements
corrctness
time
optimal(path)

security 
credit card info
or
json passed to endpoint i.e. having unnecessary column.(robustness?)

robustness
never enter no fly zone(even not shortest path/re-enter central)
(return 400 if not sure)

1.2

system&unit level testing diff?

i.e. validateorder,
system: return 200/400
unit: each part (credit card detail/restaurant/pizzacount)

integration requirement

call other endpoints correctly, i.e. fetch
				isinregion
				nextpos

performance requirement
system:
execution speed
unit:
execution speed

security requirement
system:
N/A

1.3
test approach

functional requirements: functional testing

performance: combinational testing(average)

security: ???

1.4 justify

fr: to cover all scenario 

pf: explore different cases

2 test plan

2.1

unit test -> system test
(image?) 

functional test -> combinational test (function -> performance)

2.2 evaluate

2.3 instrumentation of code
i.e. time stamp, debug message, temp-endpoint of single function.

2.4 evaluate
 
3 coverage
3.1
justify requirements

can we use external lib to produce coverage report? cite?

3.2
justify appoach

3.3
test result

3.4
evaluate result

4

