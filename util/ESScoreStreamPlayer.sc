ESScoreStreamPlayer : ScoreStreamPlayer {
  makeScore { | stream, duration = 1, event, timeOffset = 0, server |
		var ev, startTime, proto;
		proto = (
			server: server ?? this,

			schedBundle: { | lag, offset, server ... bundle |
				this.add(offset * tempo + lag + beats, bundle)
			},
			schedBundleArray: { | lag, offset, server, bundle |
				this.add(offset * tempo + lag + beats, bundle)
			}
		);

		event = event ?? { Event.default };
		event = event.copy.putAll(proto);
		beats = timeOffset;
		tempo = 1;
		bundleList = [];
		maxTime = timeOffset + duration;

		// call from a routine so that the stream has this as clock
		Routine {
			thisThread.clock = this;
			while {
				thisThread.beats = beats;
				ev = stream.next(event.copy);
				(maxTime >= beats) and: { ev.notNil }
			} {
				ev.putAll(proto);
				ev.play;
				beats = ev.delta * tempo + beats
			}
		}.next;

		bundleList = bundleList.sort { | a, b | b[0] >= a[0] };

		if((startTime = bundleList[0][0]) < 0) {
			timeOffset = timeOffset - startTime;
		};

//		bundleList.do { | b | b[0] = b[0] + timeOffset }

		^Score(bundleList)
	}
}