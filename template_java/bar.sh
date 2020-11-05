# ip netns add test
# ip netns exec test bash
# ip link set lo up

# tc qdisc add dev lo root netem 2>/dev/null
# tc qdisc change dev lo root netem delay 20ms 10ms loss 10% 20% reorder 25% 50%
# ip netns exec test

nohup ../finishedSignal.py -p 3 & nohup ../barrier.py -p 3
