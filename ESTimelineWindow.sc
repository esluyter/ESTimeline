ESTimelineWindow : Window {
  var <timeline;
  var <topPanel, <topPlug, <tempoKnob, <saveIDEButt, <loadIDEButt, <undoButt, <redoButt, <funcEditButt;
  var <scrollView, <timelineView, <trackPanelView, <rulerView;
  var playheadRout;

  *new { |name = "Timeline", bounds, timeline|
    bounds = bounds ?? Rect(0, Window.availableBounds.height - 630, Window.availableBounds.width, 630);
    ^super.new(name, bounds).acceptsMouseOver_(true).front.init(timeline);
  }

  init { |argTimeline|
    var leftPanelWidth = 80;
    var rightPanelWidth = this.bounds.width - leftPanelWidth;

    timeline = argTimeline;

    topPanel = UserView(this, Rect(0, 0, this.bounds.width, 40)).background_(Color.gray(0.8)).drawFunc_({ |view|
      Pen.addRect(Rect(0, 39, view.bounds.width, 1));
      Pen.color = Color.gray(0.5);
      Pen.fill;
    }).resize_(2);
    topPlug = UserView(this, Rect(0, 40, leftPanelWidth, 20)).background_(Color.gray(0.85)).drawFunc_({ |view|
      Pen.addRect(Rect(view.bounds.width - 1, 0, 1, view.bounds.height));
      Pen.color = Color.gray(0.7);
      Pen.fill;
    });

    tempoKnob = EZKnob(topPanel, Rect(20, 2.5, 140, 35),
      label: "Tempo (bpm)",
      controlSpec: ControlSpec(10, 500, 2, 0.0, 60.0),
      action: { |knob|
        timeline.tempo = knob.value / 60
      },
      layout: 'line2',
      initVal: timeline.tempo * 60,
    );

    funcEditButt = Button(this, Rect(210, 5, 150, 30)).states_([["Edit init/cleanup funcs"]]).action_({ ESFuncEditView(timeline); timelineView.focus });

    /*
    newButt = Button(this, Rect(50, 5, 70, 30)).states_([["New"]]);
    openButt = Button(this, Rect(125, 5, 70, 30)).states_([["Open"]]);
    saveButt = Button(this, Rect(200, 5, 70, 30)).states_([["Save"]]);
    saveAsButt = Button(this, Rect(275, 5, 70, 30)).states_([["Save As"]]);
    */

    saveIDEButt = Button(this, Rect(430, 5, 100, 30)).states_([["Open in IDE"]]).action_({
      Document.new("Timeline Score", timeline.asCompileString).front;
      timelineView.focus;
    });
    loadIDEButt = Button(this, Rect(535, 5, 100, 30)).states_([["Load from IDE"]]).action_({
      var bounds = timelineView.bounds;
      rulerView.release;
      trackPanelView.release;
      timelineView.release;
      timelineView.remove;
      timeline.free;
      timeline = Document.current.string.interpret;
      ESTimelineWindow(timeline: timeline);
      /* TODO: replace contents of same timeline
      timelineView = ESTimelineView(scrollView, bounds, timeline, duration: max(timeline.duration + 5, 60));
      rulerView.timelineView = timelineView;
      rulerView.timeline = timeline;
      trackPanelView.init(timelineView);
      this.makeDependant;
      this.makeViewDependant;
      rulerView.refresh;
      timelineView.focus;
      */
    });

    undoButt = Button(this, Rect(705, 5, 70, 30)).states_([["Undo"]]).action_({ timeline.undo; timelineView.focus; });
    redoButt = Button(this, Rect(780, 5, 70, 30)).states_([["Redo"]]).action_({ timeline.redo; timelineView.focus; });

    [saveIDEButt, loadIDEButt, funcEditButt, undoButt, redoButt, tempoKnob.numberView, tempoKnob.knobView].do { |thing|
      thing.keyDownAction_({ |...args|
        timelineView.keyDownAction.(*args);
      });
    };


    scrollView = ScrollView(this, Rect(0, 60, this.bounds.width, this.bounds.height - 60)).hasHorizontalScroller_(false).hasBorder_(false).background_(Color.gray(0.93)).resize_(5).onResize_{ |view|
      timelineView.bounds = Rect(leftPanelWidth, 0, view.bounds.width - leftPanelWidth, view.bounds.height * timelineView.heightRatio);
      timelineView.makeTrackViews;
    };
    timelineView = ESTimelineView(scrollView, Rect(leftPanelWidth, 0, rightPanelWidth, this.bounds.height - 60), timeline, duration: max(timeline.duration + 5, 60));
    trackPanelView = ESTrackPanelView(scrollView, Rect(0, 0, leftPanelWidth, this.bounds.height - 60), timelineView);
    rulerView = ESRulerView(this, Rect(leftPanelWidth, 40, rightPanelWidth, 20), timeline, timelineView).background_(Color.gray(0.97)).resize_(2);

    this.view.minHeight_(100);
    this.onClose_({ timelineView.release; });
    this.makeDependant;
    this.makeViewDependant;
  }

  makeDependant {
    timeline.addDependant { |self, what, args|
      //[what, args].postln;
      //timelineView.refresh;
      defer {
        switch (what)
        { \init } {
          timelineView.refresh;
          tempoKnob.value_(self.tempo * 60);
        }
        { \free } {
          this.close;
        }
        { \playbar } {
          timelineView.refresh;
          rulerView.refresh;
        }
        { \isPlaying } {
          if (args) { // in this case args is just true or false
            var waitTime = 30.reciprocal; // 30 fps
            playheadRout.stop; // just to make sure
            playheadRout = {
              inf.do { |i|
                timelineView.playheadView.refresh;
                if (timeline.optimizeView.not) {
                  timelineView.refresh;
                };
                waitTime.wait;
              };
            }.fork(AppClock) // lower priority clock for GUI updates
          } {
            playheadRout.stop;
            timelineView.playheadView.refresh;
          };
        }
        { \tempo } {
          tempoKnob.value_(args * 60);
        }
        { \track } {
          if ((args[2] == \mute) or: (args[2] == \solo)) {
            trackPanelView.refresh;
            timelineView.refresh;
          } {
            timelineView.trackViews[args[0]].refresh;
          };
        }
        { \tracks } {
          timelineView.makeTrackViews;
        }
        { \restoreUndoPoint } {
          timelineView.makeTrackViews;
          ESClipEditView.closeWindow;
        };
      };
    };
  }

  makeViewDependant {
    timelineView.addDependant({ |view, what, val|
      switch (what)
      { \startTime } { rulerView.refresh }
      { \duration } { rulerView.refresh }
      { \makeTrackViews } { trackPanelView.makeTrackViews }
      { \editingMode } { timelineView.refresh }
    });

    timeline.addDependant(rulerView);
  }
}