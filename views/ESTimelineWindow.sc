ESTimelineWindow : Window {
  var <timeline;
  var <topPanel, <topPlug, <tempoKnob, <newButt, <saveIDEButt, <loadIDEButt, <undoButt, <redoButt, <funcEditButt, <useParentClockBox, <snapToGridBox, <gridDivisionBox;
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

    tempoKnob = EZKnob(topPanel, Rect(20, 2.5, 110, 35),
      label: "tempoBPM",
      controlSpec: ControlSpec(10, 500, 2, 0.0, 60.0),
      action: { |knob|
        timeline.tempoBPM = knob.value
      },
      layout: 'line2',
      initVal: timeline.tempoBPM,
    );
    tempoKnob.knobView.mode_(\vert).step_(0.001).shift_scale_(5);

    funcEditButt = Button(this, Rect(160, 5, 150, 30)).states_([["Prep / Cleanup funcs"]]).action_({ ESFuncEditView(timeline); timelineView.focus });


    newButt = Button(this, Rect(350, 5, 65, 30)).states_([["New"]]).action_({ timeline.new });

    saveIDEButt = Button(this, Rect(420, 5, 100, 30)).states_([["Save As"]]).action_({
      timelineView.timelineController.saveAsDialog;
      timelineView.focus;
    });
    loadIDEButt = Button(this, Rect(525, 5, 100, 30)).states_([["Open"]]).action_({
      timelineView.timelineController.openDialog;
      timelineView.focus;
    });

    undoButt = Button(this, Rect(665, 5, 70, 30)).states_([["Undo"]]).action_({ timeline.undo; timelineView.focus; });
    redoButt = Button(this, Rect(740, 5, 70, 30)).states_([["Redo"]]).action_({ timeline.redo; timelineView.focus; });

    if (timeline.parentClip.notNil) {
      useParentClockBox = CheckBox(this, Rect(855, 10, 20, 20)).value_(timeline.parentClip.useParentClock).action_({ |view|
        timeline.parentClip.useParentClock = view.value;
        timelineView.focus;
      });
      StaticText(this, Rect(875, 10, 120, 20)).string_("useParentClock").font_(Font.sansSerif(16));
    } {  //925
      Button(this, Rect(855, 5, 200, 30)).states_([["Open as clip in new timeline"]]).action_({
        //ESTimelineWindow(bounds: this.bounds, timeline: ESTimeline([ESTrack([ESTimelineClip(0, if (timeline.duration == 0) { 10 } { timeline.duration }, timeline)])], timeline.tempo));
        //this.close;
        timeline.encapsulateSelf;
        timelineView.editingMode = false;
      })
    };

    snapToGridBox = CheckBox(this, Rect(1080, 10, 20, 20)).value_(timeline.snapToGrid).action_({ |view|
      timeline.snapToGrid = view.value;
      timelineView.focus;
    });
    StaticText(this, Rect(1100, 10, 120, 20)).string_("snapToGrid:  1 / ").font_(Font.sansSerif(16));
    gridDivisionBox = NumberBox(this, Rect(1220, 10, 70, 20)).value_(timeline.gridDivision).action_({ |view|
      timeline.gridDivision = view.value;
      timelineView.focus;
    }).clipLo_(1).decimals_(0);

    //Button(this, Rect(1200, 5, 100, 30)).states_([["Load legacy"]]).action_({timeline.restoreUndoPoint(Document.current.string, false, true); timelineView.focus;});

    [saveIDEButt, loadIDEButt, funcEditButt, undoButt, redoButt, tempoKnob.numberView, tempoKnob.knobView].do { |thing|
      thing.keyDownAction_({ |...args|
        timelineView.keyDownAction.(*args);
      });
    };


    scrollView = ScrollView(this, Rect(0, 60, this.bounds.width, this.bounds.height - 60)).hasHorizontalScroller_(false).hasBorder_(false).background_(Color.gray(0.93)).resize_(5).onResize_{ |view|
      timelineView.bounds = Rect(leftPanelWidth, 0, view.bounds.width - leftPanelWidth, view.bounds.height * timelineView.heightRatio);
      timelineView.makeTrackViews;
    };
    timelineView = ESTimelineView(scrollView, Rect(leftPanelWidth, 0, rightPanelWidth, this.bounds.height - 60), timeline, duration: max(timeline.duration + 5, 15));
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
          if ((timeline.now < timelineView.startTime) or: (timeline.now > timelineView.endTime)) {
            timelineView.startTime = timeline.now - (timelineView.duration / 6);
          };
          timelineView.refresh;
          rulerView.refresh;
        }
        { \isPlaying } {
          if (timeline.isPlaying) {
            var waitTime = 20.reciprocal; // 30 fps
            playheadRout.stop; // just to make sure
            playheadRout = {
              inf.do { |i|
                if ((timeline.now < timelineView.startTime) or: (timeline.now > timelineView.endTime)) {
                  timelineView.startTime = timeline.now - (timelineView.duration / 6);
                };
                timelineView.playheadView.refresh;
                rulerView.playheadView.refresh;
                if (timeline.optimizeView.not) {
                  timelineView.refresh;
                };
                waitTime.wait;
              };
            }.fork(AppClock) // lower priority clock for GUI updates
          } {
            playheadRout.stop;
            defer { timelineView.refresh; rulerView.playheadView.refresh };
          };
        }
        { \tempo } {
          tempoKnob.value_(timeline.tempoBPM);
        }
        { \track } {
          if (args[2] == \clip) {
            if (ESClipEditView.thisClip == args[4]) {
              if (args[5] == \startTime) {
                ESClipEditView.startTimeView.string_(args[6].asString);
                ESClipEditView.durationView.string_(args[4].duration.asString);
                if (ESClipEditView.offsetView.notNil) {
                  ESClipEditView.offsetView.string_(args[4].offset.asString);
                };
              };
              if (args[5] == \duration) {
                ESClipEditView.durationView.string_(args[6].asString);
              };
              if (args[5] == \offset) {
                ESClipEditView.offsetView.string_(args[6].asString);
              };
            };
          };

          if ((args[2] == \mute) or: (args[2] == \solo)) {
            trackPanelView.refresh;
            timelineView.refresh;
          } {
            //args.postcs;
            timelineView.trackViews[args[0]].refresh;
            //~window.timelineView.trackViews[]
          };
        }
        { \tracks } {
          //{
            //0.01.wait; // this solves a weird bug?
            timelineView.makeTrackViews;
          //}.fork(AppClock);
        }
        { \restoreUndoPoint } {
          {
            timelineView.makeTrackViews;
            ESClipEditView.closeWindow;
          }.fork(AppClock);
        }
        { \new } {
          timelineView.duration = 15;
          timelineView.startTime = -2;
        }
        { \encapsulateSelf } {
          {
            timelineView.makeTrackViews;
            ESClipEditView.closeWindow;
          }.fork(AppClock);
        }
        { \gridDivision } {
          gridDivisionBox.value = timeline.gridDivision;
          timelineView.refresh;
        }
        { \snapToGrid } {
          snapToGridBox.value = timeline.snapToGrid;
          timelineView.refresh;
        };
      };
    };
  }

  makeViewDependant {
    timelineView.addDependant({ |view, what, val|
      //[what, val].postln;
      switch (what)
      { \startTime } { rulerView.refresh }
      { \duration } { rulerView.refresh }
      { \makeTrackViews } { trackPanelView.makeTrackViews }
      { \editingMode } { timelineView.refresh }
      { \timeSelection } { timelineView.refresh; rulerView.refresh }
      { \selectedClips } { timelineView.refresh }
      { \mouseMove } { rulerView.refresh }
      { \mouseUp } { rulerView.refresh }
    });
  }
}