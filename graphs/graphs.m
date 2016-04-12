close all
clear all
M = csvread('4/jobTimes32.csv');
ids = M(:,1);
times = M(:,2);
figure
plot(ids,times);
xlabel('Jobs');
ylabel('Duration in ms');
hold on
avg = mean(times)
plot(ids, avg);
v = var(times)
count = sum(times > 2500)
perc = count/length(ids)