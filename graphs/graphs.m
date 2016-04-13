close all
clear all
M = csvread('4/jobTimes32.csv');
ids = M(:,1);
times = M(:,2);
figure
bar(ids,times);
xlabel('Job number');
ylabel('Duration in ms');
xlim([ids(1) ids(length(ids))]);
hold on
avg = mean(times)
plot(5000,'r')
v = var(times)
count = sum(times > 2500)
perc = count/length(ids)
