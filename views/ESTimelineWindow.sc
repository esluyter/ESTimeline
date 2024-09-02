ESTimelineWindow : Window {
  var <timeline;
  var <topPanel, <topPlug, <tempoKnob, <newButt, <saveIDEButt, <loadIDEButt, <undoButt, <redoButt, <funcEditButt, <useParentClockBox, <snapToGridBox, <gridDivisionBox, <useMixerChannelBox, <newTimelineClipButt;
  var <scrollView, <timelineView, <trackPanelView, <rulerView;
  var <mixerWindow;
  var playheadRout;

  *new { |name = "Timeline", bounds, timeline|
    bounds = bounds ?? Rect(0, Window.availableBounds.height - 630, Window.availableBounds.width, 630);
    ^super.new(name, bounds).acceptsMouseOver_(true).front.init(timeline);
  }

  init { |argTimeline|
    var leftPanelWidth = 105;
    var rightPanelWidth = this.bounds.width - leftPanelWidth;

    timeline = argTimeline;

    scrollView = ScrollView(this, Rect(0, 60, this.bounds.width, this.bounds.height - 60)).hasHorizontalScroller_(false).hasBorder_(false).background_(Color.gray(0.93)).resize_(5).onResize_{ |view|
      timelineView.bounds = Rect(leftPanelWidth, 0, view.bounds.width - leftPanelWidth, view.bounds.height * timelineView.heightRatio);
      timelineView.makeTrackViews;
    };
    timelineView = ESTimelineView(scrollView, Rect(leftPanelWidth, 0, rightPanelWidth, this.bounds.height - 60), timeline, duration: max(timeline.duration + 5, 15));
    timelineView.postln;
    trackPanelView = ESTrackPanelView(scrollView, Rect(0, 0, leftPanelWidth, this.bounds.height - 60), timelineView);
    rulerView = ESRulerView(this, Rect(leftPanelWidth, 40, rightPanelWidth, 20), timeline, timelineView).background_(Color.gray(0.97)).resize_(2);

    topPanel = UserView(this, Rect(0, 0, this.bounds.width, 40)).background_(Color.gray(0.8)).drawFunc_({ |view|
      Pen.addRect(Rect(0, 39, view.bounds.width, 1));
      Pen.color = Color.gray(0.5);
      Pen.fill;
    }).resize_(2);
    topPlug = UserView(this, Rect(0, 40, leftPanelWidth, 20)).background_(Color.gray(0.85)).drawFunc_({ |view|
      Pen.addRect(Rect(view.bounds.width - 1, 0, 1, view.bounds.height));
      Pen.color = Color.gray(0.7);
      Pen.fill;
    }).background_(Color.gray(/*0.86*/ 0.83));

    tempoKnob = EZKnob(topPanel, Rect(20, 2.5, 110, 35),
      label: "tempoBPM",
      controlSpec: ControlSpec(10, 500, 2, 0.0, 60.0),
      action: { |knob|
        timeline.tempoBPM = knob.value
      },
      layout: 'line2',
      initVal: timeline.tempoBPM,
    );
    tempoKnob.knobView.mode_(\vert).step_(0.001).shift_scale_(5).setContextMenuActions(
      MenuAction("Add automation envelope", {
        timelineView.timelineController.addTempoEnv;
      })
    );

    funcEditButt = Button(this, Rect(160, 5, 150, 30)).states_([["Prep / Cleanup funcs"]]).action_({ ESFuncEditView(timeline); timelineView.focus });


    newButt = Button(this, Rect(350, 5, 60, 30)).states_([["New"]]).action_({ timelineView.timelineController.new });

    saveIDEButt = Button(this, Rect(415, 5, 80, 30)).states_([["Save As"]]).action_({
      timelineView.timelineController.saveAsDialog;
      timelineView.focus;
    });
    loadIDEButt = Button(this, Rect(500, 5, 65, 30)).states_([["Open"]]).action_({
      timelineView.timelineController.openDialog;
      timelineView.focus;
    });

    //665
    undoButt = Button(this, Rect(605, 5, 70, 30)).states_([["Undo"]]).action_({ timeline.undo; timelineView.focus; });
    redoButt = Button(this, Rect(680, 5, 70, 30)).states_([["Redo"]]).action_({ timeline.redo; timelineView.focus; });

    if (timeline.parentClip.notNil) {
      useParentClockBox = CheckBox(this, Rect(785, 10, 20, 20)).value_(timeline.parentClip.useParentClock).action_({ |view|
        timeline.parentClip.useParentClock = view.value;
        tempoKnob.value = timeline.tempoBPM;
        rulerView.refresh;
        timelineView.focus;
      });
      StaticText(this, Rect(805, 10, 120, 20)).string_("useParentClock").font_(Font.sansSerif(13));

      CheckBox(this, Rect(915, 10, 20, 20)).value_(timelineView.timelineController.playParent).action_({ |view|
        timelineView.timelineController.playParent = view.value;
        timelineView.focus;
      });
      StaticText(this, Rect(935, 10, 100, 20)).string_("playParent").font_(Font.sansSerif(13));
    } {  //925
      newTimelineClipButt = Button(this, Rect(790, 5, 205, 30)).states_([["Open as clip in new timeline"]]).action_({
        if (timelineView.selectedClips.size > 0) {
          timelineView.timelineController.newTimelineClipFromSelected;
        } {
          timeline.encapsulateSelf;
          timelineView.editingMode = false;
        };
      })
    };

    snapToGridBox = CheckBox(this, Rect(1030, 10, 20, 20)).value_(timeline.snapToGrid).action_({ |view|
      timeline.snapToGrid = view.value;
      timelineView.focus;
    });
    StaticText(this, Rect(1050, 10, 120, 20)).string_("snapToGrid:  1 / ").font_(Font.sansSerif(16));
    gridDivisionBox = NumberBox(this, Rect(1170, 10, 70, 20)).value_(timeline.gridDivision).action_({ |view|
      timeline.gridDivision = view.value;
      timelineView.focus;
    }).clipLo_(1).decimals_(0);

    StaticText(this, Rect(1290, 10, 140, 20)).string_("useMixerChannel").font_(Font.sansSerif(16));
    useMixerChannelBox = CheckBox(this, Rect(1270, 10, 20, 20)).value_(timeline.useMixerChannel).action_({ |view|
      timeline.useMixerChannel = view.value;
      timelineView.focus;
    });

    Button(this, Rect(1450, 5, 25, 30)).states_([["⇤"]]).font_(Font.sansSerif(17)).action_({
      timeline.goto(0);
      timelineView.focus;
    });
    Button(this, Rect(1477.5, 5, 105, 30)).states_([["Find playhead"]]).action_({
      timelineView.timelineController.findPlayhead;
      timelineView.focus;
    });
    Button(this, Rect(1585, 5, 25, 30)).states_([["⇥"]]).font_(Font.sansSerif(17)).action_({
      timeline.goto(timeline.duration);
      timelineView.focus;
    });

    Button(this, Rect(1650, 5, 80, 30)).states_([["Init MIDI"]]).action_({
      MIDIClient.init;
      MIDIIn.connectAll;
    });

    //Button(this, Rect(1200, 5, 100, 30)).states_([["Load legacy"]]).action_({timeline.restoreUndoPoint(Document.current.string, false, true); timelineView.focus;});

    [saveIDEButt, loadIDEButt, funcEditButt, undoButt, redoButt, tempoKnob.numberView, tempoKnob.knobView].do { |thing|
      thing.keyDownAction_({ |...args|
        timelineView.keyDownAction.(*args);
      });
    };

    this.view.minHeight_(100);
    this.onClose_({ timelineView.release; if (mixerWindow.notNil) { mixerWindow.close }; });
    this.makeDependant;
    this.makeViewDependant;

    if (timeline.useMixerChannel) {
      mixerWindow = ESMixerWindow(timeline, this);
    };
  }

  makeDependant {
    timeline.addDependant { |self, what, args|
      defer {
        if (this.name.notNil) { // wtf?
          //([this.name] ++ [what, args]).postln;

          //timelineView.refresh;

          switch (what)
          { \init } {
            timelineView.refresh;
            tempoKnob.value_(self.tempo * 60);
          }
          { \free } {
            this.close;
          }
          { \playbar } {
            timelineView.timelineController.findPlayhead;
            timelineView.refresh;
            rulerView.refresh;
          }
          { \isPlaying } {
            if (timeline.isPlaying) {
              var waitTime = 20.reciprocal; // 20 fps
              playheadRout.stop; // just to make sure
              timelineView.didScroll = false;
              playheadRout = {
                inf.do { |i|
                  if (timelineView.didScroll.not) {
                    timelineView.timelineController.findPlayhead;
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
              timelineView.refresh; rulerView.playheadView.refresh;
            };
          }
          { \tempo } {
            tempoKnob.value_(timeline.tempoBPM);
            rulerView.refresh;
          }
          { \track } {
            // weirdly, was crashing if I didn't assign args[2] to a variable
            var thisThing = args[2];

            if (thisThing == \clip) {
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

            if ((thisThing == \mute) or: (thisThing == \solo) or: (thisThing == \name) or: (thisThing == \useMixerChannel)) {
              trackPanelView.refresh;
              timelineView.refresh;
            } {
              //args.postcs;
              timelineView.trackViews[args[0]].refresh;
              //~window.timelineView.trackViews[]
            };
          }
          { \useMixerChannel } {
            trackPanelView.refresh;
            if (mixerWindow.notNil) { mixerWindow.close };
            if (timeline.useMixerChannel) {
              mixerWindow = ESMixerWindow(timeline, this);
            };
          }
          { \openedFile } {
            if (mixerWindow.notNil) { mixerWindow.close };
            if (timeline.useMixerChannel) {
              mixerWindow = ESMixerWindow(timeline, this);
            };
          }
          { \initMixerChannels } {
            //if (mixer.notNil) { mixer.close };
            //mixer = MixingBoard("Mixer", nil, timeline.orderedMixerChannels);
          }
          { \tracks } {
            //{
            //0.01.wait; // this solves a weird bug?
            timelineView.makeTrackViews;
            timelineView.refresh;
            //}.fork(AppClock);
          }
          { \addUndoPoint } {
            timelineView.timelineController.saveBackup;
          }
          { \restoreUndoPoint } {
            //timelineView.timelineController.saveBackup;
            {
              snapToGridBox.value = timeline.snapToGrid;
              useMixerChannelBox.value = timeline.useMixerChannel;
              gridDivisionBox.value = timeline.gridDivision;
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
          }
          { \useMixerChannel } {
            timelineView.makeTrackViews;
          }
          { \envs } {
            timelineView.makeTrackViews;
            timelineView.refresh;
          }
          { \env } {
            timelineView.refresh;
          }
          { \hoverIndex } {
            timelineView.refresh;
          }
          { \template } {
            if (args[0] == \envs) {
              timelineView.makeTrackViews;
            };
            if (args[0] == \env) {
              timelineView.refresh;
            };
            if (args[0] == \hoverIndex) {
              timelineView.refresh;
            };
          };
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
      { \selectedClips } {
        timelineView.refresh;
        if (newTimelineClipButt.notNil) {
          newTimelineClipButt.states_([[if (timelineView.selectedClips.size > 0) { "New timelineClip from selection" } { "Open as clip in new timeline" }]])
        };
      }
      { \mouseMove } { rulerView.refresh }
      { \mouseUp } { timeline.tracks.do(_.sortClips); timelineView.refresh; rulerView.refresh }
    });
  }
}