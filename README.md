Latest working test code

```
(
~window !? { ~window.close };
~timeline.free;
~view.release;

~timeline = ESTimeline([ ESTrack([ ESClip(1.34, 6.6928763809125, nil, 0, "hello

this is a test
of the comment system.

will this be useful???
..
we will see."), ESClip(9.1205806355435, 18.57431739072, nil, -0.072697454367784, "part 1"), ESClip(28.23, 10.393658670243, nil, 0, "part 2"), ESClip(38.93, 12.023820275648, nil, 0, "outtro") ]), ESTrack([ ESPatternClip(0.0, 50.0, {Pbind(
  \instrument, \sin,
  \verbbus, ~verbbus,
  \verbamt, Penv([6, 1, 1, 6, 1, 1], [15, 5, 15, 5, inf]),
  \midinote, Pseries(
    40,
    Pwrand([1, 2, 3], [2, 5, 0.4].normalizeSum, inf)
  ).wrap(30, 120),
  \dur, Pbrown(0.1, 2) + Pwhite(-0.1, 0.1)
)}, 805771862), ESSynthClip(56.489889203478, 0.45849025486955, {'default'}, {[
  freq: ~freq
]}, {}, {'addToHead'}), ESSynthClip(57.230472977932, 0.45849025486955, {'default'}, {[
  freq: ~freq
]}, {}, {'addToHead'}), ESSynthClip(58.22036218141, 0.45849025486955, {'default'}, {[
  freq: ~freq
]}, {}, {'addToHead'}) ]), ESTrack([ ESEnvClip(13.0, 8, Env([ 0] ++ [1, 0].dup(100).flat, 0.005.dup(200) * (1, 1.04..9.0), 'sin'), {~lfoar}, offset: -2), ESPatternClip(35.89, 5.0, {Pbind(
  \instrument, \sin,
  \dur, Pfunc { ~dur }
)}, 121648384), ESSynthClip(55.346017235015, 0.45849025486955, {'default'}, {[
  freq: ~freq
]}, {}, {'addToHead'}), ESSynthClip(56.262581312309, 0.45849025486955, {'default'}, {[
  freq: ~freq
]}, {}, {'addToHead'}), ESSynthClip(57.604431121468, 0.45849025486955, {'default'}, {[
  freq: ~freq
]}, {}, {'addToHead'}), ESSynthClip(58.902285854917, 0.45849025486955, {'default'}, {[
  freq: ~freq
]}, {}, {'addToHead'}), ESSynthClip(59.547546965333, 0.45849025486955, {'default'}, {[
  freq: ~freq
]}, {}, {'addToHead'}) ]), ESTrack([ ESSynthClip(15.0, 5.0, {'default'}, {[
  amp: ~lfoar.asMap,
  freq: 100
]}, {}, {'addToHead'}), ESRoutineClip(35.74, 6.01, { 10.do {
   ~dur = rrand(0.1, 1.0);
   0.5.wait;
 }}, 1216896044, true, false, 1, {}) ]), ESTrack([ ESSynthClip(2.7364332816384, 34.076391603115, {'verb'}, {[
  verbbus: ~verbbus
]}, {}, {'addToHead'}), ESRoutineClip(55.08, 5.0, { 50.do {
   ~freq = exprand(100, 500);
   exprand(0.1, 1).wait;
 };}, 241754095, true, false, 1, {}) ]), ESTrack([ ESRoutineClip(27.39, 10.65, {~lfosyn = {
  RandSeed.kr(Impulse.kr(0), 12345);
  LFDNoise3.kr(1).exprange(100, 500);
}.play(outbus: ~lfoctrl);}, 684908290, true, false, 1, {~lfosyn.free;}) ]), ESTrack([ ESRoutineClip(9.35, 4.67, {var syn;
10.do { |i|
  s.bind { syn = Synth(\default, [freq: (40 + i).midicps]) };
  0.2.wait;
  s.bind { syn.free };
  0.2.wait;
};}, 1234300736, true, false, 1, {var syn;
s.bind { syn = Synth(\default) };
0.2.wait;
s.bind { syn.free };
}), ESSynthClip(28.29, 9.24, {'default'}, {[
  freq: ~lfoctrl.asMap,
  amp: 0.2,
  pan: 0,
]}, {}, {'addToHead'}) ]) ], 2.9658994189469, {SynthDef(\sin, { |out, freq = 100, gate = 1, amp = 0.1, preamp = 1.5, attack = 0.001, release = 0.01, pan, verbbus, verbamt, vibrato = 0.2|
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
  var env = Env.adsr(10, 0, 1, 1.0).ar(2, gate);
  var verb = NHHall.ar(in) * env;
  Out.ar(out, verb);
}).add;

~verbbus = Bus.audio(s, 2);
~lfoctrl = Bus.control(s, 1);
~lfoar = Bus.audio(s, 1);}, {~verbbus.free;
~lfoctrl.free;
~lfoar.free;});

~window = Window("Timeline", Rect(0, Window.availableBounds.height - 630, Window.availableBounds.width, 630))
.onClose_({ /*~view.release;*/ })
.acceptsMouseOver_(true).front;

~leftPanelWidth = 80;
~rightPanelWidth = ~window.bounds.width - ~leftPanelWidth;

~topPanel = UserView(~window, Rect(0, 0, ~window.bounds.width, 40)).background_(Color.gray(0.8));
~topPlug = UserView(~window, Rect(0, 40, ~leftPanelWidth, 20)).background_(Color.gray(0.85)).drawFunc_({ |view|
  Pen.addRect(Rect(view.bounds.width - 1, 0, 1, view.bounds.height));
  Pen.color = Color.gray(0.7);
  Pen.fill;
});

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
  ~trackPanelView.release;
  ~view.release;
  ~view.remove;
  ~timeline.free;
  ~timeline = Document.current.string.interpret;
  ~view = ESTimelineView(~scrollView, bounds, ~timeline);
  ~rulerView.timelineView = ~view;
  ~rulerView.timeline = ~timeline;
  ~trackPanelView.init(~view);
  ~makeDependant.();
  ~makeViewDependant.();
  ~rulerView.refresh;
  ~view.focus;
});

~undoButt = Button(~window, Rect(725, 5, 70, 30)).states_([["Undo"]]).action_({ ~timeline.undo; ~view.focus; });
~redoButt = Button(~window, Rect(800, 5, 70, 30)).states_([["Redo"]]).action_({ ~timeline.redo; ~view.focus; });

~funcEditButt = Button(~window, Rect(1000, 5, 150, 30)).states_([["Edit init/cleanup funcs"]]).action_({ ESFuncEditView(~timeline); ~view.focus });

[~newButt, ~openButt, ~saveButt, ~saveAsButt].do(_.visible_(false));
[~newButt, ~openButt, ~saveButt, ~saveAsButt, ~saveIDEButt, ~loadIDEButt, ~funcEditButt, ~undoButt, ~redoButt, ~tempoKnob.numberView, ~tempoKnob.knobView].do { |thing|
  thing.keyDownAction_({ |...args|
    ~view.keyDownAction.(*args);
  });
};

~scrollView = ScrollView(~window, Rect(0, 60, ~window.bounds.width, ~window.bounds.height - 60)).hasHorizontalScroller_(false).hasBorder_(false).background_(Color.gray(0.97));
~view = ESTimelineView(~scrollView, Rect(~leftPanelWidth, 0, ~rightPanelWidth, ~window.bounds.height - 60), ~timeline, duration:60);
~trackPanelView = ESTrackPanelView(~scrollView, Rect(0, 0, ~leftPanelWidth, ~window.bounds.height - 60), ~view);
~rulerView = ESRulerView(~window, Rect(~leftPanelWidth, 40, ~rightPanelWidth, 20), ~timeline, ~view).background_(Color.gray(0.94));

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
        if ((args[2] == \mute) or: (args[2] == \solo)) {
          ~trackPanelView.refresh;
          ~view.refresh;
        } {
          ~view.trackViews[args[0]].refresh;
        };
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
    { \makeTrackViews } { ~trackPanelView.makeTrackViews }
    { \editingMode } { ~view.refresh }
  });

  ~timeline.addDependant(~rulerView);
};
~makeDependant.();
~makeViewDependant.();
)
```
