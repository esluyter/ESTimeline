Latest working test code

```
(
~window !? { ~window.close };
~timeline.free;
~view.release;

~timeline = ESTimeline([ ESTrack([ ESPatternClip(0.0, 50.0, {Pbind(
  \instrument, \sin,
  \verbbus, ~verbbus,
  \verbamt, Penv([6, 1, 1, 6, 1, 1], [15, 5, 15, 5, inf]),
  \midinote, Pseries(
    40,
    Pwrand([1, 2, 3], [2, 5, 0.4].normalizeSum, inf)
  ).wrap(30, 120),
  \dur, Pbrown(0.1, 2) + Pwhite(-0.1, 0.1)
)}, 805771862) ]), ESTrack([ ESSynthClip(0.0, 37.51, {'verb'}, {[
  verbbus: ~verbbus
]}, {}, {'addToHead'}) ]), ESTrack([ ESRoutineClip(9.35, 4.67, {var syn;
10.do { |i|
  s.bind { syn = Synth(\default, [freq: (40 + i).midicps]) };
  0.2.wait;
  s.bind { syn.free };
  0.2.wait;
};}, 1234300736, true, false, 1, {var syn;
s.bind { syn = Synth(\default) };
0.2.wait;
s.bind { syn.free };
}), ESSynthClip(28.287937743191, 10, 'default', [ 'freq', 220, 'amp', 0.1, 'pan', 0 ]) ]) ], 2.9658994189469, {SynthDef(\sin, { |out, freq = 100, gate = 1, amp = 0.1, preamp = 1.5, attack = 0.001, release = 0.01, pan, verbbus, verbamt, vibrato = 0.2|
  var env, sig;
  var lfo = XLine.ar(0.01, vibrato, ExpRand(0.5, 2.0)) * SinOsc.ar(5.4 + (LFDNoise3.kr(0.1) * 0.5));
  gate = gate + Impulse.kr(0);
  env = Env.adsr(attack, 0.1, 0.4, release).ar(2, gate);
  sig = SinOsc.ar(freq * lfo.midiratio) * env;
  sig = (sig * preamp).tanh;
  sig = Pan2.ar(sig, pan, amp);
  Out.ar(out, sig);
  Out.ar(verbbus, sig * verbamt);
}).add;

SynthDef(\testSustain, { |out, freq = 122, sustain = 1, amp = 0.1, pan = 0|
  var sig = Pan2.ar(Gendy1.ar(), pan);
  var env = Env.sine(sustain).ar(2);
  Out.ar(out, sig * env * amp);
}).add;

SynthDef(\verb, { |out, verbbus, gate = 1|
  var in = In.ar(verbbus, 2);
  var env = Env.adsr(0.01, 0, 1, 1.0).ar(2, gate);
  var verb = NHHall.ar(in) * env;
  Out.ar(out, verb);
}).add;

~verbbus = Bus.audio(s, 2);}, {~verbbus.free});

~window = Window("Timeline", Rect(0, Window.availableBounds.height - 630, Window.availableBounds.width, 630))
.onClose_({ /*~view.release;*/ })
.acceptsMouseOver_(true).front;

~topPanel = UserView(~window, Rect(0, 0, ~window.bounds.width, 40)).background_(Color.gray(0.75));

~tempoKnob = EZKnob(~topPanel, Rect(~topPanel.bounds.width - 200, 0, 140, 40),
  label: "Tempo (bpm)",
  controlSpec: ControlSpec(10, 500, 2, 0.0, 60.0),
  action: { |knob|
    ~timeline.tempo = knob.value / 60
  },
  layout: 'line2',
  initVal: ~timeline.tempo * 60,
);
~tempoKnob.knobView.mode = \vert;

~newButt = Button(~window, Rect(50, 5, 70, 30)).states_([["New"]]);
~openButt = Button(~window, Rect(125, 5, 70, 30)).states_([["Open"]]);
~saveButt = Button(~window, Rect(200, 5, 70, 30)).states_([["Save"]]);
~saveAsButt = Button(~window, Rect(275, 5, 70, 30)).states_([["Save As"]]);

~saveIDEButt = Button(~window, Rect(380, 5, 100, 30)).states_([["Open in IDE"]]).action_({
  Document.new("Timeline Score", ~timeline.asCompileString).front;
});
~loadIDEButt = Button(~window, Rect(485, 5, 100, 30)).states_([["Load from IDE"]]).action_({
  var bounds = ~view.bounds;
  ~rulerView.release;
  ~view.release;
  ~view.remove;
  ~timeline.free;
  ~timeline = Document.current.string.interpret;
  ~view = ESTimelineView(~scrollView, bounds, ~timeline);
  ~rulerView.timelineView = ~view;
  ~rulerView.timeline = ~timeline;
  ~makeDependant.();
  ~makeViewDependant.();
  ~rulerView.refresh;
});

~undoButt = Button(~window, Rect(725, 5, 70, 30)).states_([["Undo"]]).action_({ ~timeline.undo });
~redoButt = Button(~window, Rect(800, 5, 70, 30)).states_([["Redo"]]).action_({ ~timeline.redo });

~funcEditButt = Button(~window, Rect(1000, 5, 150, 30)).states_([["Edit init/cleanup funcs"]]).action_({ ESFuncEditView(~timeline) });

[~newButt, ~openButt, ~saveButt, ~saveAsButt].do(_.visible_(false));
[~newButt, ~openButt, ~saveButt, ~saveAsButt, ~saveIDEButt, ~loadIDEButt, ~funcEditButt, ~undoButt, ~redoButt, ~tempoKnob.numberView, ~tempoKnob.knobView].do { |thing|
  thing.keyDownAction_({ |...args|
    ~view.keyDownAction.(*args);
  });
};


~scrollView = ScrollView(~window, Rect(0, 60, ~window.bounds.width, ~window.bounds.height - 60)).hasHorizontalScroller_(false).hasBorder_(false);
~view = ESTimelineView(~scrollView, Rect(0, 0, ~window.bounds.width, ~window.bounds.height - 60), ~timeline, duration:60);
~rulerView = ESRulerView(~window, Rect(0, 40, ~window.bounds.width, 20), ~timeline, ~view);

~makeDependant = {
  ~timeline.addDependant { |self, what, args|
    //[what, args].postln;
    //~view.refresh;
    defer {
      switch (what)
      { \init } {
        ~view.refresh;
        ~tempoKnob.value_(self.tempo * 60);
      }
      { \free } {
        /*~window.close;*/
      }
      { \playbar } {
        ~view.refresh;
        ~rulerView.refresh;
      }
      { \tempo } {
        ~tempoKnob.value_(args * 60);
      }
      { \track } {
        ~view.trackViews[args[0]].refresh;
      }
      { \tracks } {
        ~view.makeTrackViews;
      }
      { \restoreUndoPoint } {
        ~view.makeTrackViews;
        ESClipEditView.closeWindow;
      };
    };
  };
};
~makeViewDependant = {
  ~view.addDependant({ |view, what, val|
    switch (what)
    { \startTime } { ~rulerView.refresh }
    { \duration } { ~rulerView.refresh }
  });

  ~timeline.addDependant(~rulerView);
};
~makeDependant.();
~makeViewDependant.();
)
```
