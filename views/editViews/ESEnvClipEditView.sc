ESEnvClipEditView : ESClipEditView {

  *new { |clip, timeline|
    var panelFont = Font("Helvetica", 16);
    var busView, makeBusBox, makeBusRateMenu, targetView, addActionView, useLiveInputBox, liveInputMenu, ccNumField, channelMenu, channelText, smoothBox, smoothText, recordArmButt, codeView, minView, maxView, curveView, isExponentialBox, keepBreakpointValuesBox;
    var adjustBg = {
      busView.background_(if (makeBusBox.value) { Color.gray(0.8) } { Color.white });
    };

    this.prNew(clip, timeline, {
      var env = ("{" ++ codeView.string ++ "}").interpret;
      if (env.isNil) {
        ESBulkEditWindow.ok
      } {
        clip.env = env;
        clip.target = ("{" ++ targetView.string ++ "}").interpret;
        clip.addAction = ("{" ++ addActionView.string ++ "}").interpret;
        clip.color = colorView.background;
        clip.startTime = startTimeView.string.interpret;
        clip.duration =  durationView.string.interpret;
        clip.offset = offsetView.string.interpret;

        clip.armed = recordArmButt.value.asBoolean;
        clip.useLiveInput = useLiveInputBox.value;
        clip.liveInput = liveInputMenu.value;
        clip.ccNum = ccNumField.string.interpret;
        clip.midiChannel = channelMenu.value;
        clip.midiSmooth = smoothBox.value;

        if (keepBreakpointValuesBox.value.not) {
          // keep Env the same but change breakpoint values bc of range adjustment.
          clip.min = minView.value;
          clip.max = maxView.value;
          clip.curve = curveView.value;
          clip.isExponential = isExponentialBox.value;
        } {
          // adjust Env to keep breakpoint values
          var min = minView.value;
          var max = maxView.value;
          var curve = curveView.value;
          var isExponential = isExponentialBox.value;

          // only do this if there's a change
          if ((clip.min != min) or: (clip.max != max) or: (clip.curve != curve) or: (clip.isExponential != isExponential)) {
            if ((isExponential and: ((min.sign != max.sign))).not) {
              var thisEnv = env.value;
              var oldLevels = thisEnv.levels;
              var values = oldLevels.collect(clip.prValueScale(_));
              var newLevels;
              clip.min = min;
              clip.max = max;
              clip.curve = curve;
              clip.isExponential = isExponential;
              newLevels = values.collect(clip.prValueUnscale(_));
              clip.env = Env(newLevels, thisEnv.times, thisEnv.curves);
              codeView.string_(clip.env.asESDisplayString);
            };
          };
        };

        clip.makeBusRate = makeBusRateMenu.item.asSymbol;
        clip.makeBus = makeBusBox.value;
        if (clip.makeBus.not) {
          clip.bus = ("{" ++ busView.string ++ "}").interpret;
        };
        clip.name = nameField.string.asSymbol;

        timeline.addUndoPoint;
      };
    });

    StaticText(editorWindow, Rect(20, 30, 180, 20)).string_("bus").font_(panelFont);
    busView = TextField(editorWindow, Rect(10, 50, 270, 40)).string_(if (clip.makeBus.not) { clip.bus.asESDisplayString } { "" }).font_(Font.monospace(16));

    StaticText(editorWindow, Rect(285, 30, 100, 30)).string_(". . . or:");
    makeBusBox = CheckBox(editorWindow, Rect(335, 30, 20, 20)).value_(clip.makeBus).action_(adjustBg);
    StaticText(editorWindow, Rect(360, 30, 200, 20)).string_("makeBus with rate:").font_(panelFont);
    makeBusRateMenu = PopUpMenu(editorWindow, Rect(335, 50, 180, 30)).items_(["audio", "control"]).value_([\audio, \control].indexOf(clip.makeBusRate));

    StaticText(editorWindow, Rect(20, 100, 180, 20)).string_("target").font_(panelFont);
    targetView = TextField(editorWindow, Rect(10, 120, 230, 40)).string_(clip.target.asESDisplayString).font_(Font.monospace(16));

    StaticText(editorWindow, Rect(255, 100, 180, 20)).string_("addAction").font_(panelFont);
    addActionView = TextField(editorWindow, Rect(245, 120, 270, 40)).string_(clip.addAction.asESDisplayString).font_(Font.monospace(16));

    View(editorWindow, Rect(535, 20, 1, 160)).background_(Color.gray(0.5, 0.3));

    useLiveInputBox = CheckBox(editorWindow, Rect(555, 30, 20, 20)).value_(clip.useLiveInput).action_({ |view| recordArmButt.visible_(view.value) });
    StaticText(editorWindow, Rect(580, 30, 180, 20)).string_("use live input:").font_(panelFont);
    recordArmButt = Button(editorWindow, Rect(690, 25, 100, 25)).states_([["record arm"], ["record armed", Color.gray(1), Color.hsv(0, 0.75, 0.75)]]).font_(Font.sansSerif(13)).value_(clip.armed).visible_(clip.useLiveInput);

    channelText = StaticText(editorWindow, Rect(555, 100, 160, 30)).string_("MIDI Channel #").align_(\right);
    channelMenu = PopUpMenu(editorWindow, Rect(720, 100, 80, 30)).items_((0..15) ++ \all).value_(clip.midiChannel);
    smoothText = StaticText(editorWindow, Rect(555, 135, 160, 30)).string_("Smoothing").align_(\right);
    smoothBox = NumberBox(editorWindow, Rect(720, 135, 80, 30)).value_(clip.midiSmooth).step_(0.1).scroll_step_(0.1).shift_scale_(10);
    ccNumField = TextField(editorWindow, Rect(720, 65, 80, 30)).string_(clip.ccNum).visible_(false);
    liveInputMenu = PopUpMenu(editorWindow, Rect(555, 65, 160, 30)).items_(["Mouse X", "Mouse Y", "MIDI Control #", "MIDI Pitch Bend", "MIDI Note", "MIDI Mono Note", "MIDI Velocity", "MIDI Gated Velocity"]).action_({ |view|
      if (view.value == 2) {
        ccNumField.visible = true;
      } {
        ccNumField.visible = false;
      };
      if (view.value >= 2) {
        channelText.visible = true;
        channelMenu.visible = true;
        smoothText.visible = true;
        smoothBox.visible = true;
        smoothBox.value = if (view.value <= 3) { 0.1 } { 0 };
      } {
        channelText.visible = false;
        channelMenu.visible = false;
        smoothText.visible = false;
        smoothBox.visible = false;
      }
    }).valueAction_(clip.liveInput);

    StaticText(editorWindow, Rect(20, 175, 50, 20)).string_("env").font_(panelFont);
    StaticText(editorWindow, Rect(50, 177, 480, 20)).string_("... edit code if you must or use cmd-e for mouse breakpoint editor mode").font_(Font.sansSerif(13));
    codeView = CodeView(editorWindow, Rect(10, 200, 790, 400)).font_(Font.monospace(16)).string_(clip.env.asESDisplayString).background_(Color.gray(0.8));
    if (timeline.useEnvir) {
      codeView.interpretEnvir_(timeline.envir);
    };

    StaticText(sidePanel, Rect(0, 300, 180, 20)).string_("min").font_(panelFont);
    minView = NumberBox(sidePanel, Rect(0, 320, 180, 20)).font_(Font.monospace(16)).value_(clip.min);
    StaticText(sidePanel, Rect(0, 350, 180, 20)).string_("max").font_(panelFont);
    maxView = NumberBox(sidePanel, Rect(0, 370, 180, 20)).font_(Font.monospace(16)).value_(clip.max);
    StaticText(sidePanel, Rect(0, 400, 180, 20)).string_("curve").font_(panelFont);
    curveView = NumberBox(sidePanel, Rect(0, 420, 180, 20)).font_(Font.monospace(16)).value_(clip.curve);
    isExponentialBox = CheckBox(sidePanel, Rect(0, 450, 20, 20)).value_(clip.isExponential);
    StaticText(sidePanel, Rect(20, 450, 150, 20)).string_("isExponential").font_(panelFont);
    keepBreakpointValuesBox = CheckBox(sidePanel, Rect(0, 480, 20, 20)).value_(true);
    StaticText(sidePanel, Rect(20, 475, 150, 30)).string_("keep breakpoint values when adjusting range").font_(panelFont.copy.size_(12));

    adjustBg.()
  }
}