run-with-my-probcli:
	PROB_HOME=~/git_root/prob_prolog/ gradle run
refresh:
	PROB_HOME=~/git_root/prob_prolog/ gradle run  --refresh-dependencies