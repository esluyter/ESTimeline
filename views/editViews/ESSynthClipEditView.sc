ESSynthClipEditView : ESClipEditView {

  *new { |clip, timeline|
    var panelFont = Font.sansSerif(16);
    var defNameView, targetView, addActionView, argsView, funcView, doPlayFuncBox;
    var openIDE, loadIDE;
    var adjustBg = {
      funcView.visible_(doPlayFuncBox.value);
      openIDE.visible_(doPlayFuncBox.value);
      loadIDE.visible_(doPlayFuncBox.value);
      defNameView.background_(if (doPlayFuncBox.value) { Color.gray(0.8) } { Color.white });
      targetView.background_(if (timeline.useMixerChannel and: clip.track.useMixerChannel) { Color.gray(0.8) } { Color.white });
      addActionView.background_(if (timeline.useMixerChannel and: clip.track.useMixerChannel) { Color.gray(0.8) } { Color.white });
    };

    this.prNew(clip, timeline, {
      var func = funcView.string.interpret;
      if (func.isNil) {
        "func is nil".postln;
        ESBulkEditWindow.ok
      } {
        clip.func = func;
      };
      clip.doPlayFunc = doPlayFuncBox.value;
      clip.name = nameField.string.asSymbol;
      clip.args = argsView.value;
      clip.defName = ("{" ++ defNameView.string ++ "}").interpret;
      clip.target = ("{" ++ targetView.string ++ "}").interpret;
      clip.addAction = ("{" ++ addActionView.string ++ "}").interpret;
      clip.color = colorView.background;
      clip.startTime = startTimeView.string.interpret;
      clip.duration =  durationView.string.interpret;

      argsView.free;
      argsView.init(clip.args.copy, clip.argControls, clip);

      timeline.addUndoPoint;
    });

    StaticText(editorWindow, Rect(40, 10, 180, 20)).string_("defName").font_(panelFont);
    StaticText(editorWindow, Rect(320, 10, 100, 20)).string_(". . . or:");
    defNameView = TextField(editorWindow, Rect(30, 30, 320, 40)).string_(clip.defName.asESDisplayString).font_(Font.monospace(14));

    StaticText(editorWindow, Rect(390, 10, 120, 20)).string_("doPlayFunc").font_(panelFont);
    doPlayFuncBox = CheckBox(editorWindow, Rect(370, 10, 20, 20)).value_(clip.doPlayFunc).action_(adjustBg);
    funcView = CodeView(editorWindow, Rect(370, 30, 430, 570)).font_(Font.monospace(12)).string_(clip.func.asCompileString);

    StaticText(editorWindow, Rect(20, 90, 180, 20)).string_("target").font_(panelFont);
    targetView = TextField(editorWindow, Rect(10, 110, 170, 30)).string_(clip.target.asESDisplayString).font_(Font.monospace(14));

    StaticText(editorWindow, Rect(195, 90, 180, 20)).string_("addAction").font_(panelFont);
    addActionView = TextField(editorWindow, Rect(185, 110, 175, 30)).string_(clip.addAction.asESDisplayString).font_(Font.monospace(14));

    StaticText(editorWindow, Rect(170, 165, 180, 20)).string_("args").font_(panelFont);
    Button(editorWindow, Rect(145, 165, 20, 20)).states_([["⟳"]]).font_(Font.sansSerif(30)).action_({
      argsView.freeArgControls;
      argsView.initArgControls(SynthDescLib.at(if (doPlayFuncBox.value) { clip.autoDefName } { defNameView.string.interpret }).controls);
    });
    argsView = ESArgsView(editorWindow, Rect(10, 190, 350, 410), clip);

    openIDE = Button(sidePanel, Rect(0, 410, 180, 25)).string_("Open in IDE").action_({
      Document.new("Edit Synth Clip", funcView.string).promptToSave_(false).front;
    });
    loadIDE = Button(sidePanel, Rect(0, 440, 180, 25)).string_("Copy from IDE").action_({
      funcView.string_(Document.current.string);
    });

    adjustBg.value;
  }
}


ESArgsView : ScrollView {
  var args, argControls, clip;
  var argPairs;
  var defaultArgs;

  var control_i;

  *new { |parent, bounds, clip|
    ^super.new(parent, bounds).hasBorder_(false).init(clip.args.copy, clip.argControls, clip);
  }

  initArgControls { |argargControls|
    var i = control_i;
    argControls = argargControls;
    defaultArgs = [];
    argControls.do { |cn|
      if (args.indexOf(cn.name).isNil) {
        var index = defaultArgs.size;
        var action = { |view, x, y, mods, buttNum, clickCount|
          if (clickCount > 1) {
            args = args.add(cn.name).add(cn.defaultValue);
            this.free;
            this.init(args, argControls, clip);
          };
        };
        defaultArgs = defaultArgs.add([
          StaticText(this, Rect(10, 2.5 + (i * 30), 140, 25)).string_(cn.name).align_(\right).font_(Font().italic_(true)).stringColor_(Color.gray(0.5)).mouseDownAction_(action),
          StaticText(this, Rect(160, 2.5 + (i * 30), this.bounds.width - 165, 25)).string_(cn.defaultValue).stringColor_(Color.gray(0.5)).mouseDownAction_(action)
        ]);
        i = i + 1;
      };
    };
  }

  init { |argargs, argargControls, argclip|
    var i = 0;
    args = argargs;
    clip = argclip;
    argPairs = [];
    args.pairsDo { |key, val, index|
      var action = { |view, x, y, mods, buttNum, clickCount|
        if (clickCount > 1) {
          args.removeAt(index);
          args.removeAt(index);
          this.free;
          this.init(args, argControls, clip);
        };
      };
      argPairs = argPairs.add([
        StaticText(this, Rect(10, 2.5 + (i * 30), 140, 25)).string_(key).align_(\right).mouseDownAction_(action).setContextMenuActions(
          MenuAction("Add Env for Synth argument", {
            clip.track.timeline.timelineController.prAddEnvForSynth(clip, key);
            this.free;
            this.init(clip.args.copy, clip.argControls, clip);
          })
        ),
        TextField(this, Rect(160, 2.5 + (i * 30), this.bounds.width - 165, 25)).string_(val.asESDisplayString).font_(Font.monospace(14)).keyUpAction_({ |view|
          args[index + 1] = ("{" ++ view.string ++ "}").interpret;
        })
      ]);
      i = i + 1;
    };

    control_i = i;
    this.initArgControls(argargControls);
  }

  freeArgControls {
    defaultArgs.flat.do(_.remove);
  }

  free {
    argPairs.flat.do(_.remove);
    this.freeArgControls;
  }

  value {
    var ret = [];
    argPairs.do { |pair|
      ret = ret.add(pair[0].string.asSymbol).add(("{" ++ pair[1].string ++ "}").interpret);
    };
    ^ret;
  }
}