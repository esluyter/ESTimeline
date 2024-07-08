ESEnvClip : ESClip {
  var <env, <bus, <>target, <>addAction, <min, <max, <>curve, <>isExponential, <makeBus = false, <>makeBusRate;
  var <synth;
  var <hoverIndex, <editingFirst, <originalCurve, <curveIndex;

  classvar <buses;  // event format name -> [bus, nClips] -- when nClips becomes 0 bus should be freed.

  min_ { |val| min = val; this.changed(\min); }
  max_ { |val| max = val; this.changed(\max); }
  env_ { |val| env = val; this.changed(\env); }
  bus_ { |val| bus = val; this.changed(\bus); }
  rate { ^this.bus.value.rate }
  makeBus_ { |val|
    if (val != makeBus) {
      this.cleanup;
      makeBus = val;
      this.prep;
      this.changed(\makeBus);
    };
  }


  name_ { |val|
    if (val != name) {
      this.cleanup;

      if (val.asString.size > 0) {
        name = val;
      } {
        name = nil;
      };

      this.prep;
      this.changed(\name, val);
    };

  }


  hoverIndex_ { |val|
    if (val != hoverIndex) {
      hoverIndex = val;
      this.changed(\hoverIndex);
    };
  }

  *initClass {
    var sizes;

    buses = ();

    sizes = [2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8190]; // 8191 crashes server
    ServerBoot.add {
      sizes.do { |n|
        [\kr, \ar].do { |rate|
          [\curve, \exp].do { |type|
            SynthDef(('ESEnvClip_' ++ rate ++ '_' ++ type ++ '_' ++ n).asSymbol, { |out, gate = 1, tempo = 1, min = 0, max = 1|
              var env = \env.kr(Env(0.dup(n), 1.dup(n - 1), 0.dup(n - 1)));
              var sig = EnvGen.perform(rate, env, timeScale: tempo.reciprocal);
              switch (type)
              {\curve} {
                sig = sig.lincurve(0, 1, min, max, \curve.kr(0));
              }
              {\exp} {
                sig = sig.linexp(0, 1, min, max);
              };
              FreeSelf.kr(gate <= 0);
              Out.perform(rate, out, sig);
            }).add;
          };
        };
      };
    };
  }

  *new { |startTime, duration, offset = 0, color, name, env, bus, target, addAction = 'addToHead', min = 0, max = 1, curve = 0, isExponential = false, makeBus = true, makeBusRate = \audio, mute = false, prep = false|
    ^super.new(startTime, duration, offset, color, name, mute: mute).init(env, bus, target, addAction, min, max, curve, isExponential, makeBus, makeBusRate, prep);
  }

  storeArgs { ^[startTime, duration, offset, color, name, env, bus, target, addAction, min, max, curve, isExponential, makeBus, makeBusRate, mute] }

  duplicate {
    //this.asCompileString.interpret.track_(track);
    ^this.class.new(*(this.storeArgs ++ true)).track_(track); // prep is true
  }

  init { |argEnv, argBus, argTarget, argAddAction, argMin, argMax, argCurve, argExp, argMakeBus, argMakeBusRate, argPrep|
    env = argEnv;
    bus = argBus;
    target = argTarget;
    addAction = argAddAction;
    min = argMin;
    max = argMax;
    curve = argCurve;
    isExponential = argExp;
    makeBusRate = argMakeBusRate;
    makeBus = argMakeBus;
    if (argPrep) { this.prep };
  }

  prep {
    if (makeBus) {
      //"allocating bus".postln;
      if (name.notNil) {
        // for named clip, allocate a global bus or increase its clip count
        if (buses[name].notNil) {
          buses[name][1] = buses[name][1] + 1;
        } {
          buses[name] = [Bus.perform(makeBusRate, Server.default, 1), 1];
        };
        bus = buses[name][0];
      } {
        // for unnamed clip, always make new bus
        bus = Bus.perform(makeBusRate, Server.default, 1);
      }
    };
  }

  cleanup {
    if (makeBus) {
      if (bus.notNil) { // if bus is nil, we've already cleaned up
        //"freeing bus".postln;
        if (name.notNil) {
          // for named clip, free or decrement the global bus
          if (buses[name].notNil) {
            buses[name][1] = buses[name][1] - 1;
            if (buses[name][1] < 1) {
              bus.free;
              buses[name] = nil;
            };
          };
        } {
          // for unnamed clip, always free the bus you made
          if (bus.index.notNil) { bus.free; };
        };
      };
    };
    bus = nil;
  }

  prStop {
    Server.default.bind { synth.release };
    synth = nil;
  }

  prStart { |startOffset = 0.0, clock|
    var thisEnv = this.envToPlay(startOffset);
    var size = thisEnv.levels.size.nextPowerOfTwo;
    if (size > 8190) {
      "Envelope can have max 8190 points. Please adjust.".warn;
      size = 8190;
    };
    if (bus.value.notNil) {
      var defName = if (this.rate == 'control') { 'ESEnvClip_kr' } { 'ESEnvClip_ar' };
      defName = (defName ++ if (this.isExponential) { "_exp_" } { "_curve_" } ++ size).asSymbol;
      Server.default.bind {
        synth = Synth(defName, [env: thisEnv, out: bus.value, tempo: clock.tempo, min: min, max: max, curve: curve], target.value, addAction.value);
      };
    };
  }

  prTempoChanged { |tempo|
    if (synth.notNil) {
      Server.default.bind {
        synth.set(\tempo, tempo);
      };
    };
  }

  prDraw { |left, top, width, height, editingMode, clipLeft, clipWidth|
    var pratio = duration / width;
    var tratio = pratio.reciprocal;
    var line = this.bus.asESDisplayString ++ "  -> " ++ bus.value.asCompileString;
    var minString = min.asString;
    var maxString = max.asString;
    var minWidth, maxWidth;
    var font = Font.monospace(10);

    Pen.use {
      // TODO: make a good envelope drawing that doesn't freeze gui
      /*
      var n = min(duration * 30, width);
      var points = this.envToPlay.discretize(n);
      Pen.moveTo(left@(top + ((1 - env[offset]) * height)));
      points.do { |level, i|
        Pen.lineTo((left + i.linlin(0, n, 0, width))@(top + ((1 - level) * height)));
      };
      */
      /*
      var scale = ((height) / 50).asInteger.max(1);
      scale.postln;
      Pen.moveTo(left@(top + ((1 - env[offset]) * height)));
      (width / scale).asInteger.do { |i|
        i = i * scale;
        Pen.lineTo((left + i)@(top + ((1 - env[offset + (i * pratio)]) * height)));
      };
      */
      /*
      var thisEnv = this.envToPlay;
      var time = 0;
      Pen.moveTo(left@(top + ((1 - env[offset]) * height)));
      thisEnv.levels.do { |level, i|
        Pen.lineTo((left + (time * tratio))@(top + ((1 - level) * height)));
        if (i < thisEnv.times.size) {
          time = time + thisEnv.times[i];
        };
      };
      */
      var n = (width / (height / 50).max(1)).asInteger;
      var nratio = width / n;
      var firstI, lastI, prevY;
      var thisEnv = env.value;
      firstI = ((0 - left) / nratio).asInteger.clip(0, n);
      lastI = ((Window.screenBounds.width - left) / nratio).asInteger.clip(0, n);
      prevY = top + ((1 - thisEnv[offset + (firstI * pratio)]) * height);
      (firstI..lastI).do { |i|
        var thisY, thisX;
        i = i * nratio;
        thisX = left + i;
        thisY = top + ((1 - thisEnv[offset + (i * pratio)]) * height);
        Pen.addRect(Rect(thisX, min(prevY, thisY), nratio, max(1, abs(prevY - thisY))));
        prevY = thisY;
      };

      Pen.color = Color.gray(1, 0.8);
      Pen.fill;
    };

    if (editingMode) {
      var thisEnv = this.envToPlay;
      var points = this.envBreakPoints(thisEnv, left, top, width, height);
      points.do { |point, i|
        Pen.addOval(Rect(point.x - 3, point.y - 3, 6, 6));
        Pen.strokeColor = Color.white;
        if (i == hoverIndex) {
          var val = this.valueAtIndex(i);
          Pen.stringAtPoint(val.asString, point.x@(if (point.y - 20 < top) { point.y + 20 } { point.y - 20 }), Font.sansSerif(15), Color.white);
          Pen.fillColor = Color.white;
        } {
          Pen.fillColor = this.color;
        };
        Pen.width = 2;
        Pen.fillStroke;
      };
    };

    if (left < 0) {
      width = width + left;
      left = 0;
    };
    if (clipLeft.notNil and: { left < clipLeft }) {
      width = width - (clipLeft - left);
      left = clipLeft;
    };

    if (editingMode.not and: (height > 50)) {
      line = ESStringShortener.trim(line, width - 5, font);
      Pen.stringAtPoint(line, (left+3.5)@(top+20+(0 * 10)), font, Color.gray(1.0, 0.5));
    };

    if (width > 50) {
      # maxString, maxWidth = ESStringShortener.trimWidth(maxString, width - 30, font);
      Pen.stringAtPoint(maxString, (left + width - maxWidth - 2)@(top), font, Color.gray(1.0, 0.5));

      # minString, minWidth = ESStringShortener.trimWidth(minString, width - 5, font);
      Pen.stringAtPoint(minString, (left + width - minWidth - 2)@(top + height - 11), font, Color.gray(1.0, 0.5));
    };

    ^this.prTitle;
  }

  prTitle { ^"Env" }

  prMouseMove { |x, y, xDelta, yDelta, mods, left, top, width, height|
    var thisEnv = this.envToPlay;
    var points;
    if (hoverIndex == 0) {
      env = Env([thisEnv.levels[0]] ++ thisEnv.levels, [0] ++ thisEnv.times, if (thisEnv.curves.isArray) { [thisEnv.curves[0]] ++ thisEnv.curves } { thisEnv.curves });
      offset = 0;
      hoverIndex = 1;
      editingFirst = true;
      thisEnv = this.envToPlay;
    };
    points = this.envBreakPoints(thisEnv, left, top, width, height);
    if (hoverIndex.notNil) {
      // adjust breakpoint
      var prevPoint = points[max(0, hoverIndex - 1)];
      var nextPoint = if (hoverIndex < (points.size - 1)) { points[hoverIndex + 1] } { (left + width)@0 };
      var adjustedX = x.clip(prevPoint.x, nextPoint.x).clip(left, left + width);
      var adjustedY = y.clip(top, top + height);
      points[hoverIndex] = adjustedX@adjustedY;
      if (editingFirst) { points[0] = left@adjustedY; };
      #env, offset = this.envFromBreakPoints(points, left, top, width, height);
      this.changed(\env);
    } {
      // adjust curve if not over breakpoint and no modifiers
      if (mods == 0) {
        var curves = if (thisEnv.curves.isArray) { thisEnv.curves } { thisEnv.curves.dup(thisEnv.times.size) };
        curveIndex = curveIndex ?? this.segmentIndex(points, x@y);
        originalCurve = originalCurve ?? curves[curveIndex];
        if (originalCurve.isNumber) {
          var slope = thisEnv.levels[curveIndex + 1] - thisEnv.levels[curveIndex];
          curves[curveIndex] = originalCurve + (yDelta * 0.1 * slope.sign);
          env = Env(thisEnv.levels, thisEnv.times, curves);
          this.changed(\env);
        };
      };
    };
  }

  prMouseDown { |x, y, mods, buttNum, clickCount, left, top, width, height|
    editingFirst = false;
    originalCurve = nil;
    curveIndex = nil;

    if (mods.isShift) {
      var pratio = duration / width;
      var tratio = pratio.reciprocal;
      var thisEnv = this.envToPlay;
      var thisTime = ((x - left) * pratio);
      var thisLevel = thisEnv[thisTime];
      var levels = [thisEnv.levels[0]];
      var times = [];
      var curves = [];
      var time = 0;
      var inserted = false;

      var curve;
      var level;
      thisEnv.times.do { |timeDiff, i|
        curve = if (thisEnv.curves.isArray) { thisEnv.curves[i] } { thisEnv.curves };
        level = thisEnv.levels[i + 1];
        if (inserted.not) {
          if ((time + timeDiff) > thisTime) {
            var ratio = (thisTime - time) / timeDiff;
            levels = levels.add(thisLevel);
            times = times.add(thisTime - time);
            if (curve.isNumber) {
              curves = curves.add(curve * ratio);
              curves = curves.add(curve * (1 - ratio));
            } {
              curves = curves.add(curve);
              curves = curves.add(curve);
            };
            levels = levels.add(level);
            times = times.add(time + timeDiff - thisTime);
            inserted = true;
          } {
            levels = levels.add(level);
            times = times.add(timeDiff);
            curves = curves.add(curve);
          };
          time = time + timeDiff;
        } {
          levels = levels.add(level);
          times = times.add(timeDiff);
          curves = curves.add(curve);
        }
      };
      if (inserted.not) {
        levels = levels.add(thisLevel);
        times = times.add(thisTime - time);
        curves = curves.add(curve);
      };
      this.env = Env(levels, times, curves);
      offset = 0;
    };

    if (mods.isAlt and: hoverIndex.notNil) {
      var thisEnv = this.envToPlay;
      var levels = [if (hoverIndex > 0) { thisEnv.levels[0] } { thisEnv.levels[1] }];
      var times = [];
      var curves = [];
      var time = 0;
      var addTime;
      thisEnv.times.do { |timeDiff, i|
        var curve = if (thisEnv.curves.isArray) { thisEnv.curves[i] } { thisEnv.curves };
        var level = thisEnv.levels[i + 1];
        if (i == (hoverIndex - 1)) {
          addTime = timeDiff;
        } {
          if (addTime.notNil) {
            timeDiff = timeDiff + addTime;
            addTime = nil;
          };
          levels = levels.add(level);
          times = times.add(timeDiff);
          curves = curves.add(curve);
        };
      };
      hoverIndex = nil;
      this.env = Env(levels, times, curves);
      offset = 0;
    };
  }

  prHover { |x, y, hoverTime, left, top, width, height|
    var thisEnv = this.envToPlay;
    var points = this.envBreakPoints(thisEnv, left, top, width, height);
    this.hoverIndex = this.nearestBreakPointIndex(points, (x@y));
  }

  prHoverLeave {
    this.hoverIndex = nil;
  }

  defaultColor { ^Color.hsv(0.58, 0.45, 0.65, 0.7) }

  guiClass { ^ESEnvClipEditView }

  envToPlay { |startOffset = 0|
    var thisEnv = env.value;
    var playOffset = offset + startOffset;
    var initlevel = thisEnv[playOffset];

    var levels = thisEnv.levels;
    var times = thisEnv.times;
    var curves = thisEnv.curves;

    var thisDuration = duration - startOffset;

    if (playOffset < 0) {
      levels = [levels[0]] ++ levels;
      times = [playOffset * -1] ++ times;
      curves = if (curves.isArray) { [curves[0]] ++ curves } { curves };
    };
    if (playOffset > 0) {
      var runningtime = 0.0;
      var i = 0;
      while { (runningtime < playOffset) && (i < times.size) } {
        runningtime = runningtime + times[i];
        i = i + 1;
      };
      if (runningtime >= playOffset) { // passed target
        var timetokeep = runningtime - playOffset;
        levels = levels[(i - 1)..levels.size];
        times = times[(i - 1)..times.size];
        curves = if (curves.isArray) { curves[(i - 1)..curves.size] } { curves };
        times[0] = timetokeep;
        levels[0] = initlevel;
      } { // ran out of envelope
        times = [0];
        levels = initlevel ! 2;
        curves = \lin;
      };
    };

    if (times.sum > thisDuration) {
      var endLevel = Env(levels, times, curves)[thisDuration];
      var prevTime = 0;
      var runningtime = 0.0;
      var i = 0;
      while { (runningtime < thisDuration) && (i < times.size) } {
        prevTime = runningtime;
        runningtime = runningtime + times[i];
        i = i + 1;
      };
      if (i > 0) {
        i = i - 1;
      };
      levels = levels[0..i+1];
      times = times[0..i];
      curves = if (curves.isArray) { curves[0..i] } { curves };

      times[i] = thisDuration - prevTime;
      levels[i+1] = endLevel;
    };

    ^Env(levels, times, curves);
  }

  envBreakPoints { |thisEnv, left, top, width, height|
    var pratio = duration / width;
    var tratio = pratio.reciprocal;
    var time = 0;
    var points = [];
    thisEnv.levels.do { |level, i|
      var x = left + (time * tratio);
      var y = top + ((1 - level) * height);
      points = points.add(x@y);
      if (i < thisEnv.times.size) {
        time = time + thisEnv.times[i];
      };
    };
    ^points;
  }

  envFromBreakPoints { |points, left, top, width, height|
    var pratio = duration / width;
    var tratio = pratio.reciprocal;
    var curves = this.envToPlay.curves;
    var times = [];
    var levels = [1 - ((points[0].y - top) / height)];
    var offset = (points[0].x - left) * pratio;
    var time = offset;
    points[1..].do { |point|
      var timeDiff = ((point.x - left) * pratio) - time;
      var level = 1 - ((point.y - top) / height);
      times = times.add(timeDiff);
      levels = levels.add(level);
      time = time + timeDiff;
    }
    ^[Env(levels, times, curves), -1 * offset];
  }

  nearestBreakPointIndex { |points, thisPoint, tolerance = 5|
    var nearestIndex = nil;
    var nearestDistance = inf;
    points.do { |point, i|
      var distance = dist(point, thisPoint);
      if ((distance < tolerance) and: (distance < nearestDistance)) {
        nearestIndex = i;
      };
    };
    ^nearestIndex;
  }

  segmentIndex { |points, thisPoint|
    points[1..].do { |point, i|
      if ((points[i].x < thisPoint.x) and: (point.x > thisPoint.x)) { ^i };
    };
  }

  valueNow {
    ^this.valueAtTime(track.timeline.now - startTime);
  }

  valueAtIndex { |i|
    ^this.prValueScale(this.envToPlay.levels[i]);
  }

  valueAtTime { |time|
    ^this.prValueScale(this.envToPlay.at(time));
  }

  prValueScale { |level|
    if (isExponential) {
      ^level.linexp(0.0, 1.0, min, max);
    } {
      ^level.lincurve(0.0, 1.0, min, max, curve);
    };
  }

  prValueUnscale { |value|
    if (isExponential) {
      ^value.explin(min, max, 0.0, 1.0);
    } {
      ^value.curvelin(min, max, 0.0, 1.0, curve);
    };
  }

  hasEditingMode { ^true }

  asMap { ^bus.asMap }

  /*
  embedInStream { |inval|
    ^this.asMap.embedInStream(inval);
  }
  */
  asStream {
    ^Routine({ arg inval;
      loop {
        this.track.timeline[this.name].embedInStream(inval)
      }
    })
  }
  embedInStream { arg inval;
    //loop {
      { this.asMap }.value(inval).embedInStream(inval);
    //};
    ^inval;
	}

  /*
  asStream { /*^Routine { loop { this.asMap.yield } }*/
    ^Pn(Plazy { this.asMap }, inf).asStream; // so it will update with new envelopes/buses as they become current
  }
  */

  + { ^this }
}