# Virtual Grid Scheduler
The virtual grid scheduler

## Launch!
Launch with a manifest file as argument.

This file consists of:
* grid scheduler ids to start
* resource manager ids to start
* users to start
* nodes per cluster
* repositories path

Example:
```
0 1 2
0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19
0 1 2 3 4
1000
/home/user/src/grid/src/main/resources
```

The repositories path should point to a directory with the files:
* users
* gss
* rms

Each of these files consists of lines with an id and an `RMI` url.

## Assembly
Create the jar with `sbt assembly`.