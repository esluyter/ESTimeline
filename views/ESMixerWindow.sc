/*
The code is not pretty but the mixer interface is!
The only thing that doesn't work is the mixer channel output bus (will reset to default).
*/
ESMixerWindow {
  var <timeline, <timelineWindow;
  var <window, <waitWin;
  var <scrollView, <outViews, <peaks, <sliders, <dbViews, <panViews, <insertUserViews, <insertScrollViews;
  var mixerRout;
  var channelIndexMap;
  var dependantFunc;

  var <oscFunc;

  classvar <panSpec, <faderSpec;

  *initClass {
    // this try is necessary in case MixerChannelDef is not installed
    try {
      Class.initClassTree(MixerChannelDef);
      StartUp.add {
        MixerChannelDef(\mix1x1, 1, 1,
          fader: SynthDef("mixers/Mxb1x1", {
            arg busin, busout, level, clip = 2;
            var in, bad, badEG;
            in = In.ar(busin, 1) * level;
            // On server quit, `clip` may be set to 0 before processing is finished
            // In that case, spurious out-of-range warnings are produced
            clip = max(1, clip);
            bad = CheckBadValues.ar(in, post: 0) + (8 * (in.abs > clip));
            SendReply.ar(bad, '/mixerChBadValue', [bad, in].flat, 0);
            badEG = EnvGen.ar(Env(#[1, 0, 1], #[0, 0.05], releaseNode: 1), bad);
            in = Select.ar(bad > 0, [in * badEG, DC.ar(0)]);
            SendPeakRMS.ar(in, 20, 3, "/mixerChannel", busin);
            // so that mixerchan bus can be used as postsendbus
            ReplaceOut.ar(busin, in);
            Out.ar(busout, in);
          }, [nil, nil, 0.08]),
          controls: (
            level: (spec: \amp, value: 0.75),
            clip: (spec: [0.1, 100, \exp], value: 20.dbamp)
          )
        );

        MixerChannelDef(\mix1x2, 1, 2,
          fader: SynthDef("mixers/Mxb1x2", {
            arg busin, busout, level, pan, clip = 2;
            var in, out, bad, badEG;
            in = In.ar(busin, 1) * level;
            clip = max(1, clip);
            bad = CheckBadValues.ar(in, post: 0) + (8 * (in.abs > clip));
            SendReply.ar(bad, '/mixerChBadValue', [bad, in].flat, 0);
            badEG = EnvGen.ar(Env(#[1, 0, 1], #[0, 0.05], releaseNode: 1), bad);
            in = Select.ar(bad > 0, [in * badEG, DC.ar(0)]);
            out = Pan2.ar(in, pan);
            SendPeakRMS.ar(out, 20, 3, "/mixerChannel", busin);
            // so that mixerchan bus can be used as postsendbus
            ReplaceOut.ar(busin, out);
            Out.ar(busout, out);
          }, [nil, nil, 0.08, 0.08]),
          controls: (level: (spec: \amp, value: 0.75),
            pan: \bipolar,
            clip: (spec: [0.1, 100, \exp], value: 20.dbamp)
          )
        );

        MixerChannelDef(\mix2x2, 2, 2,
          fader: SynthDef("mixers/Mxb2x2", {
            arg busin, busout, level, pan, clip = 2;
            var in, l, r, out, bad, badEG, silent = DC.ar(0);
            in = In.ar(busin, 2) * level;
            clip = max(1, clip);
            bad = (CheckBadValues.ar(in, post: 0) + (8 * (in.abs > clip)));
            SendReply.ar(bad, '/mixerChBadValue', [bad, in, clip].flat, 0);
            bad = bad.sum;
            badEG = EnvGen.ar(Env(#[1, 0, 1], #[0, 0.05], releaseNode: 1), bad);
            bad = bad > 0;
            #l, r = in.collect { |chan| Select.ar(bad, [chan * badEG, silent]) };
            out = Balance2.ar(l, r, pan) * 1.4142135623731; // to offset balance2 center dip
            SendPeakRMS.ar(out, 20, 3, "/mixerChannel", busin);
            ReplaceOut.ar(busin, out);
            Out.ar(busout, out);
          }, [nil, nil, 0.08, 0.08]),
          controls: (level: (spec: \amp, value: 0.75),
            pan: \bipolar,
            clip: (spec: [0.1, 100, \exp], value: 20.dbamp)
          )
        );
      };
    };

    faderSpec = ControlSpec(0.0, 4.0, 4);//ControlSpec(0.0, 2, \amp);
    panSpec = \pan.asSpec;
  }

  *new { |timeline, timelineWindow|
    ^super.newCopyArgs(timeline, timelineWindow).init;
  }

  // for flattening mixer channel names and timeline ids
  mcnFunc { |arr|
    var index = arr[0];
    var ret = [];
    arr[1..].do { |item|
      if (item.isArray.not) {
        ret = ret.add([item, index])
      } {
        ret = ret ++ this.mcnFunc(item);
      };
    };
    ^ret;
  }
  mcfFunc { |arr, level = 0|
    var ret = [];
    arr.do { |item|
      if (item.isArray.not) {
        ret = ret.add([item, level])
      } {
        ret = ret ++ this.mcfFunc(item, level + 1);
      };
    };
    ^ret;
  }

  initOSCFunc {
    if (oscFunc.notNil) { oscFunc.free };
    oscFunc = OSCFunc({ |msg|
      var oscMsg, synthId, busIndex, thisPeaks, powers;
      var index;
      //msg.postln;
      # oscMsg, synthId, busIndex = msg;
      # thisPeaks, powers = msg[3..].clump(2).flop;
      index = channelIndexMap[busIndex];
      //[index, peaks, powers].postln;
      defer {
        //[busIndex, index].postln;
        if (index.notNil and: { peaks[index].notNil }) {
          thisPeaks.size.do { |i|
            peaks[index][i].value = powers[i].ampdb.linlin(-60, 0, 0, 1);
            peaks[index][i].peakLevel = thisPeaks[i].ampdb.linlin(-60, 0, 0, 1);
          };
        };
      };
    }, '/mixerChannel');
  }

  freeOSCFunc {
    oscFunc.free;
  }

  cmdPeriod {
    this.initOSCFunc;
  }

  init {
    var width = 1500, height = 600;
    var left = Window.availableBounds.width - width;
    if (window.notNil) { window.close };
    window = Window("Mixer", Rect(left, 0, width, height)).background_(Color.gray(0.55)).front.onClose_({
      this.freeOSCFunc;
      timeline.removeDependant(this);
      CmdPeriod.remove(this);
    });

    // OSC func for channel metering
    this.initOSCFunc;
    CmdPeriod.add(this); // so oscFunc is restored on cmd-.
    timeline.addDependant(this);

    this.buildMixer;
  }

  update { |self, what, args|
    var updateAutomatedLevels = {
      var mixerChannelNamesFlat = this.mcnFunc(timeline.orderedMixerChannelNames);
      mixerChannelNamesFlat.do { |arr, i| var name = arr[0]; var id = arr[1];
        var timeline = ESTimeline.at(id);
        var template = timeline.mixerChannelTemplates[name];
        panViews[i][0].enabled_(template.envs.pan.isNil);
        if (template.envs.pan.notNil) {
          var pan = template.envs.pan.valueAtTime(timeline.soundingNow);
          var panString = (pan.abs * 100).asInteger.asString ++ " " ++ if (pan.isPositive) { "R" } { "L" };
          if (pan == 0) { panString = "C" };
          panViews[i][0].value = panSpec.unmap(pan);
          panViews[i][1].string = panString;
        };
        sliders[i].enabled_(template.envs.level.isNil);
        if (template.envs.level.notNil) {
          var level = template.envs.level.valueAtTime(timeline.soundingNow);
          sliders[i].value = faderSpec.unmap(level);
          dbViews[i].value = level.ampdb;
        };
      };

      insertUserViews.flat.do(_.refresh);
    };

    //[self, what, args].postln;

    defer {
      switch (what)
      { \addUndoPoint } {
        "addUndoPoint".postln;
      }
      { \restoreUndoPoint } {
        if (timeline.useMixerChannel.not) {
          // this means initMixerChannels will not be called
          this.buildMixer;
        };
      }
      { \free } {
        window.close;
      }
      { \beginInitMixerChannels } {
        if (waitWin.isNil) {
          var bounds = timelineWindow.bounds;
          waitWin = Window("please wait", bounds).alpha_(0.5).front;
          StaticText(waitWin, bounds.copy.origin_(0@0)).string_("loading MixerChannels").align_(\center).font_(Font.monospace.size_(40));
        };
      }
      { \endInitMixerChannels } {
        this.buildMixer;
        if (waitWin.notNil) {
          waitWin.close;
          waitWin = nil;
        };
      }
      { \playbar } {
        this.buildMixer;
      }
      { \tracks } {
        this.buildMixer;
      }
      { \track } {
        if (args.indexOf(\tracks).notNil) {
          this.buildMixer;
        };
        if (args.indexOf(\beginInitMixerChannels).notNil) {
          if (waitWin.isNil) {
            var bounds = timelineWindow.bounds;
            waitWin = Window("please wait", bounds).alpha_(0.5).front;
            StaticText(waitWin, bounds.copy.origin_(0@0)).string_("loading MixerChannels").align_(\center).font_(Font.monospace.size_(40));
          };
        };
        if (args.indexOf(\endInitMixerChannels).notNil) {
          this.buildMixer;
          if (waitWin.notNil) {
            waitWin.close;
            waitWin = nil;
          };
        };
        if (args.indexOf(\template).notNil) {
          var i = args.indexOf(\template);
          var next = args[i + 1];
          if ((next == \envs) or: (next == \env)) {
            updateAutomatedLevels.();
          };
        };
      }
      { \template } {
        if ((args[0] == \envs) or: (args[0] == \env)) {
          updateAutomatedLevels.();
        };
      }
      { \isPlaying } {
        var names;

        if (args) {
          var waitTime = 20.reciprocal; // 5 fps refresh mixer
          names = timeline.orderedMixerChannelNames;
          mixerRout.stop; // just to make sure
          mixerRout = {
            inf.do {
              var nowNames = timeline.orderedMixerChannelNames;
              if (nowNames != names) {
                names = nowNames;
                this.buildMixer;
              } {
                // update automated levels here
                updateAutomatedLevels.();
              };
              waitTime.wait;
            };
          }.fork(AppClock) // lower priority clock for GUI updates
        } {
          mixerRout.stop;
          this.buildMixer;
        };
      }
    }
  }

  buildMixer {
    var width = window.bounds.width, height = window.bounds.height;
    var left = Window.availableBounds.width - width;
    var top;
    var levelAdjust = 10;
    var meterHeight = 250;
    var dbHeight = 20;
    var panHeight = 30;
    var outHeight = 20;
    var nameHeight = 40;
    var muteHeight = 22;
    var insertHeight = 15;
    var trackWidth = 80;

    var mixerChannels = timeline.orderedMixerChannels;
    var mixerChannelNames = timeline.orderedMixerChannelNames;
    // [1, \melody, \harmony, [2, \bass, \kik, \sn, \master], \drums, \fx]
    // mixerChannelNames[i]

    var mixerChannelNamesFlat = this.mcnFunc(mixerChannelNames);
    var mixerChannelsFlat = this.mcfFunc(mixerChannels);

    var leftOffset = if (mixerChannelsFlat.size * trackWidth + 25 < width) {
      width - (mixerChannelsFlat.size * trackWidth + 25)
    } { 0 };

    scrollView.remove;
    scrollView = ScrollView(window, Rect(leftOffset, 0, width - leftOffset, height)).hasBorder_(false).background_(Color.gray(0.725));

    if (mixerChannels == [nil]) { ^false }; // this will happen if server not booted

    channelIndexMap = ();
    mixerChannelsFlat.do { |arr, i| var mc = arr[0]; /*if (mc.notNil) { */channelIndexMap[mc.inbus.index] = i /*}*/ };

    // in case of problem just don't throw infinite error messages..
    //try {

      top = height;

      mixerChannelsFlat.do { |arr, i| var mc = arr[0]; var level = arr[1];
        var bounds = Rect(i * trackWidth + 14, level * 7.5, trackWidth - 3, height - 3 - (level * levelAdjust) - (level * 7.5));
        var name = mixerChannelNamesFlat[i][0];
        var color = Color.gray(0.88 - (level * 0.015));
        UserView(scrollView, bounds).background_(color);
      };

      //make sure right side margin is drawn
      View(scrollView, Rect(mixerChannels.size * trackWidth + 14, 0, 11, height));

      // names
      top = top - nameHeight - 5;
      mixerChannelsFlat.do { |arr, i| var mc = arr[0]; var level = arr[1];
        var name = mixerChannelNamesFlat[i][0];
        var bounds = Rect(i * trackWidth + 15, top - (level * levelAdjust), trackWidth - 5, 40);
        var color = Color.gray(0.9 - (level * 0.015));
        StaticText(scrollView, bounds).align_(\center).string_(name).font_(Font.sansSerif(12, true)).stringColor_(Color.gray(0.5)).background_(color);
        // draw folder indicators
        if ((i > 0) and: { mixerChannelsFlat[i - 1][1] > level }) {
          UserView(scrollView, Rect(i * trackWidth + 7 - levelAdjust, top + nameHeight - (mixerChannelsFlat[i - 1][1] * levelAdjust), levelAdjust + 3, levelAdjust)).drawFunc_({ |view|
            Pen.moveTo(0@0);
            Pen.lineTo(levelAdjust@levelAdjust);
            Pen.lineTo(view.bounds.width@levelAdjust);
            Pen.lineTo(view.bounds.width@0);
            Pen.lineTo(0@0);
            Pen.color = color;
            Pen.fill;
          })//.background_(Color.red);
        };
      };

      // out bus
      top = top - outHeight - 2.5;
      outViews = mixerChannelsFlat.collect { |arr, i| var mc = arr[0]; var level = arr[1];
        var bounds = Rect(i * trackWidth + 15, top - (level * levelAdjust), trackWidth - 5, outHeight);
        PopUpMenu(scrollView, bounds).items_(BusDict.menuItems(Server.default)).font_(Font.sansSerif(10, true)).background_(Color.gray(0.65)).stringColor_(Color.gray(0.95)).action_({ |view|
          mc.outbus = view.value;
        }).value_(mc.outbus.index);
      };

      // mute/record
      top = top - muteHeight -3;
      mixerChannelsFlat.do { |arr, i| var mc = arr[0]; var level = arr[1];
        [
          Button(scrollView, Rect(i * trackWidth + 25, top - (level * levelAdjust), muteHeight, muteHeight)).states_([
            ["⚫︎" /*◉︎*/, Color.gray(0.6), Color.gray(0.8)],
            ["⚫︎", Color.red, Color.black]])
          .focusColor_(Color.clear).font_(Font.sansSerif(16, true)).action_({ |view|
            if (view.value.asBoolean) {
              mc.startRecord;
              if (timeline.isPlaying.not) {
                timeline.play;
              };
            } {
              mc.stopRecord;
            };
          }).value_(mc.isRecording),
          Button(scrollView, Rect(i * trackWidth + 58, top - (level * levelAdjust), muteHeight, muteHeight)).states_([
            ["M", Color.gray(0.55), Color.gray(0.8)],
            ["M", Color.gray(0.7), Color.gray(0.3)]])
          .focusColor_(Color.clear).font_(Font.sansSerif(16, true)).action_({ |view|
            mc.mute(view.value.asBoolean);
          }).value_(mc.muted),
        ]
      };

      // fader
      top = top - meterHeight - 5;
      peaks = mixerChannelsFlat.collect { |arr, i| var mc = arr[0]; var level = arr[1];
        var levelWidth = 35 / mc.inChannels;
        mc.inChannels.collect { |j|
          LevelIndicator(scrollView, Rect((i * trackWidth) + (j * levelWidth) + 20, top, levelWidth - 3, meterHeight - (level * levelAdjust)) ).warning_(0.9).critical_(0.99)
          .drawsPeak_(true)
          .numTicks_(0)
          .numMajorTicks_(0)
          .meterColor_(Color.hsv(0.3, 0.7, 0.99))
          .warningColor_(Color.hsv(0.15, 0.6, 1))
          .background_(Color.gray(0.6));
        };
      };

      sliders = mixerChannelNamesFlat.collect { |arr, i| var name = arr[0]; var id = arr[1];
        var timeline = ESTimeline.at(id);
        var mc = timeline.mixerChannels[name];
        var template = timeline.mixerChannelTemplates[name];
        var level = mixerChannelsFlat[i][1];
        var thisMeterHeight = meterHeight - (level * levelAdjust);
        var bounds = Rect((i * trackWidth) + 55, top - 1, 30, thisMeterHeight + 2);
        var thisLevel = if (template.envs.level.notNil) { template.envs.level.valueAtTime(timeline.soundingNow) } { mc.level };
        var slider = Slider(scrollView, bounds).background_(Color.gray(0.8)).value_(faderSpec.unmap(thisLevel)).action_({ |view|
          timeline.setMixerChannel(name, \level, faderSpec.map(view.value));
        }).mouseDownAction_({ |view, x, y, mods, buttNum, clickCount|
          if (clickCount > 1) {
            timeline.setMixerChannel(name, \level, 1);
            true;
          } { false }
        }).enabled_(template.envs.level.isNil)
        .setContextMenuActions(
          MenuAction("Add automation envelope", {
            var unmappedLevel = faderSpec.unmap(mc.level);
            template.envs.level = ESMixerChannelEnv(Env(unmappedLevel.dup(2), [0], [0]), faderSpec.minval, faderSpec.maxval, 4); // <- this curve could be issue, assumes faderSpec will always have curve 4...
            timeline.addUndoPoint;
          });
        ).mouseUpAction_({
          timeline.addUndoPoint
        });

        UserView(scrollView, bounds).drawFunc_({
          var color = Color.gray(0.5);
          Pen.stringAtPoint("+12", 5@5, Font.sansSerif(10), color);
          Pen.stringAtPoint("+6", 5@((1 - faderSpec.unmap(2)) * thisMeterHeight + 4), Font.sansSerif(10), color);
          Pen.stringAtPoint("0", 5@((1 - faderSpec.unmap(1)) * thisMeterHeight), Font.sansSerif(10), color);
          Pen.stringAtPoint("-6", 5@((1 - faderSpec.unmap(0.5)) * thisMeterHeight - 4), Font.sansSerif(10), color);
          Pen.stringAtPoint("-12", 5@((1 - faderSpec.unmap(0.25)) * thisMeterHeight - 6), Font.sansSerif(10), color);
          Pen.stringAtPoint("-20", 5@((1 - faderSpec.unmap(0.1)) * thisMeterHeight - 8), Font.sansSerif(10), color);
          Pen.stringAtPoint("-inf", 5@(thisMeterHeight - 12), Font.sansSerif(10), color);
        }).acceptsMouse_(false);
        slider;
      };

      top = top - dbHeight - 2;
      dbViews = mixerChannelNamesFlat.collect { |arr, i| var name = arr[0]; var id = arr[1];
        var timeline = ESTimeline.at(id);
        var mc = timeline.mixerChannels[name];
        var template = timeline.mixerChannelTemplates[name];
        var bounds = Rect(i * trackWidth + 20, top, trackWidth - 15, dbHeight);
        var thisLevel = if (template.envs.level.notNil) { template.envs.level.valueAtTime(timeline.soundingNow) } { mc.level };
        NumberBox(scrollView, bounds).background_(Color.gray(0.85)).normalColor_(Color.gray(0.4)).font_(Font.sansSerif(11)).value_(thisLevel.ampdb).align_(\center).action_({ |view|
          //mc.level = view.value.dbamp
          timeline.setMixerChannel(name, \level, view.value.dbamp);
        }).scroll_step_(0.05).shift_scale_(5).ctrl_scale_(2.5).enabled_(template.envs.level.isNil);
      };

      top = top - panHeight - 5;
      //panViews = mixerChannelsFlat.collect { |arr, i| var mc = arr[0];
      panViews = mixerChannelNamesFlat.collect { |arr, i| var name = arr[0]; var id = arr[1];
        var timeline = ESTimeline.at(id);
        var mc = timeline.mixerChannels[name];
        var template = timeline.mixerChannelTemplates[name];
        var bounds = Rect(i * trackWidth + 55, top, panHeight, panHeight);
        var pan = if (template.envs.pan.notNil) { template.envs.pan.valueAtTime(timeline.soundingNow) } { mc.pan };
        var panString = (pan.abs * 100).asInteger.asString ++ " " ++ if (pan.isPositive) { "R" } { "L" };
        if (pan == 0) { panString = "C" };
        [
          Knob(scrollView, bounds).value_(panSpec.unmap(pan)).centered_(true).mode_(\vert).step_(0.0025).action_({ |view|
            timeline.setMixerChannel(name, \pan, panSpec.map(view.value));
          }).mouseDownAction_({ |view, x, y, mods, buttNum, clickCount|
            if (clickCount > 1) { timeline.setMixerChannel(name, \pan, 0); true } { nil };
          }).enabled_(template.envs.pan.isNil).setContextMenuActions(
            MenuAction("Add automation envelope", {
              var unmappedLevel = panSpec.unmap(mc.pan);
              template.envs.pan = ESMixerChannelEnv(Env(unmappedLevel.dup(2), [0], [0]), panSpec.minval, panSpec.maxval); // <- this curve could be issue, assumes faderSpec will always have curve 4...
              timeline.addUndoPoint;
            });
          ).mouseUpAction_({
            timeline.addUndoPoint;
          }),
          StaticText(scrollView, bounds.copy.left_(i * trackWidth + 20, top, 30, panHeight)).align_(\right).string_(panString).font_(Font.sansSerif(10, true)).stringColor_(Color.gray(0.5));
        ];
      };



      // inserts
      insertScrollViews = [];
      insertUserViews = [];
      mixerChannelNamesFlat.do { |arr, i| var name = arr[0]; var id = arr[1];
        var level = mixerChannelsFlat[i][1];
        var color = Color.gray(0.88 - (level * 0.015));
        var insertView = ScrollView(scrollView, Rect(i * trackWidth + 20, level * 7.5, trackWidth - 15, top - 10 - (level * 7.5))).hasBorder_(false).background_(color);
        var timeline = ESTimeline.at(id);
        var template = timeline.mixerChannelTemplates[name];
        var funcViewFactory = { |bounds, index|
          StaticText(insertView, bounds)
          .string_(" { }")
          .background_(Color.gray(0.6))
          .font_(Font.monospace(9))
          .stringColor_(Color.gray(0.8))
          .mouseDownAction_({ |view, x, y, mods, buttNum, clickCount|
            if (clickCount > 1) {
              // edit fx clip
              var clip = template.fx[index];
              clip.guiClass.new(clip, timeline, template, index);
            };
          }).setContextMenuActions(
            MenuAction("Delete", {
              template.fx.removeAt(index);
              timeline.addUndoPoint;
              this.buildMixer;
            })
          );
        };
        var sendViewFactory = { |bounds, index, method, stringColor, dbColor, barColor|
          var clickPoint, clickVal;

          UserView(insertView, bounds).background_(Color.gray(0.75)).drawFunc_({ |view|
            var levelPx, dbString, dbStringWidth;
            var send = template.perform(method)[index].copy;
            if (template.envs.perform(method)[index].notNil) {
              send[1] = template.envs.perform(method)[index].valueAtTime(timeline.soundingNow);
            };
            levelPx = faderSpec.unmap(send[1]) * view.bounds.width;
            dbString = send[1].ampdb.round(0.1).asString;
            dbStringWidth = QtGUI.stringBounds(dbString, Font.sansSerif(8)).width + 2;
            Pen.addRect(Rect(0, 0, levelPx, view.bounds.height));
            Pen.color = barColor.copy.alpha_(if (template.envs.perform(method)[index].isNil) { 1 } { 0.5 });
            Pen.fill;
            Pen.stringAtPoint(send[0].asCompileString, 2@2, Font.monospace(9), stringColor);
            Pen.stringAtPoint(dbString, (view.bounds.width - dbStringWidth)@4, Font.sansSerif(8), dbColor);
          }).mouseDownAction_({ |view, x, y, mods, buttNum, clickCount|
            if (clickCount > 1) {
              var wasPre = method == 'preSends';
              var send = template.perform(method)[index].copy;
              ESBulkEditWindow.keyValue("Edit Send:", "name", send[0].asCompileString, "db", send[1].ampdb.asCompileString, "pre fade", wasPre, callback: { |name, level, pre|
                var arr = [name.interpret, level.interpret.dbamp];
                if (pre) {
                  if (wasPre) {
                    template.preSends[index] = arr;
                  } {
                    template.postSends.removeAt(index);
                    template.preSends = template.preSends.add(arr);
                  };
                } {
                  if (wasPre) {
                    template.preSends.removeAt(index);
                    template.postSends = template.postSends.add(arr);
                  } {
                    template.postSends[index] = arr;
                  }
                };
                timeline.initMixerChannels;
              });
            } {
              clickPoint = x@y;
              clickVal = template.perform(method)[index][1];
            };
          }).mouseUpAction_({
            timeline.addUndoPoint;
          }).mouseMoveAction_({ |view, x, y, mods|
            // only adjust if there's no automation
            if (template.envs.perform(method)[index].isNil) {
              var yDelta = clickPoint.y - y;
              var step = 0.005;
              var val;
              if (mods.isAlt) {
                step = step * 0.2;
              };
              if (mods.isCmd) {
                step = step * 2;
              };
              val = faderSpec.map(faderSpec.unmap(clickVal) + (yDelta * step));
              template.perform(method)[index][1] = val;
              timeline.mixerChannels[name].perform(method)[index].level = val;
              view.refresh;
            };
          }).setContextMenuActions(
            MenuAction(if (template.envs.perform(method)[index].isNil) { "Add automation envelope" } { "Remove automation envelope" }, {
              var arr = template.envs.perform(method);
              var unmappedLevel = faderSpec.unmap(template.perform(method)[index][1]);
              while { (arr.size - 1) < index } {
                arr = arr.add(nil);
              };
              // add envelope if there's not one already there, otherwise remove it
              if (arr[index].isNil) {
                arr[index] = ESMixerChannelEnv(Env(unmappedLevel.dup(2), [0], [0]), faderSpec.minval, faderSpec.maxval, 4); // <- this curve could be issue, assumes faderSpec will always have curve 4...
                template.envs.perform((method ++ "_").asSymbol, arr);
              } {
                var val = arr[index].valueAtTime(timeline.soundingNow);
                arr[index].stop; arr[index] = nil;
                template.envs.perform((method ++ "_").asSymbol, arr);
                template.perform(method)[index][1] = val;
                timeline.mixerChannels[name].perform(method)[index].level = val;
              };
              this.buildMixer;
            }),
            MenuAction("Delete", {
              var arr = template.envs.perform(method);
              template.perform(method).removeAt(index);
              if ((arr.size - 1) >= index) {
                arr.removeAt(index);
                template.envs.perform((method ++ "_").asSymbol, arr);
              };
              timeline.initMixerChannels;
            })
          );
        };
        var userViews = [];

        max(((top - 20) / (insertHeight + 5)).asInteger, template.fx.size + template.preSends.size + template.postSends.size).do { |j|
          var bounds = Rect(0, j * (insertHeight + 5) + 10, trackWidth - 15, insertHeight);
          if (j < template.fx.size) {
            userViews = userViews.add(funcViewFactory.(bounds, j));
          } {
            if (j < (template.fx.size + template.preSends.size)) {
              var index = j - template.fx.size;
              userViews = userViews.add(sendViewFactory.(bounds, index, 'preSends', Color.gray(0.9), Color.white, Color.gray(0.5)));
            } {
              if (j < (template.fx.size + template.preSends.size + template.postSends.size)) {
                var index = j - template.fx.size - template.preSends.size;
                userViews = userViews.add(sendViewFactory.(bounds, index, 'postSends', Color.gray(0.3), Color.gray(0.5), Color.white));
              } {
                UserView(insertView, bounds)
                .background_(Color.gray(0.82)).setContextMenuActions(
                  MenuAction("New Insert FX", {
                    var newClip = ESFxSynth(func: {
  var sig = In.ar(~out, 2);
  sig;
}, doPlayFunc: true).prep;
                    newClip.guiClass.new(newClip, timeline, template, template.fx.size);
                    template.fx = template.fx.add(newClip);
                    this.buildMixer;
                  }),
                  MenuAction("New Send", {
                    ESBulkEditWindow.keyValue("New Send:", "name", "'verb'", "db", 0.0, "pre fade", callback: { |name, level, pre|
                      if (pre) {
                        template.preSends = template.preSends.add([name.interpret, level.interpret.dbamp]);
                      } {
                        template.postSends = template.postSends.add([name.interpret, level.interpret.dbamp]);
                      };
                      timeline.initMixerChannels;
                    });
                  }),
                );
              };
            };
          };
        };

        insertScrollViews = insertScrollViews.add(insertView);
        insertUserViews = insertUserViews.add(userViews);
      };

      mixerChannelsFlat.do { |arr| var mc = arr[0];
        mc.removeDependant(dependantFunc);
      };
      dependantFunc = { |mc, what, args|
        var i = channelIndexMap[mc.inbus.index];
        if (i.notNil) {
          //[i, what, args].postln;
          if (what[\what] == \control) {
            if (what[\name] == \level) {
              sliders[i].value = faderSpec.unmap(mc.level);
              dbViews[i].value = mc.level.ampdb;
            };
            if (what[\name] == \pan) {
              var panString = (mc.pan.abs * 100).asInteger.asString ++ " " ++ if (mc.pan.isPositive) { "R" } { "L" };
              if (mc.pan == 0) { panString = "C" };
              panViews[i][0].value = panSpec.unmap(mc.pan);
              panViews[i][1].string_(panString);
            }
          };
        };
      };
      mixerChannelsFlat.do { |arr| var mc = arr[0];
        mc.addDependant(dependantFunc);
      };
    //}; // end try
  }
}