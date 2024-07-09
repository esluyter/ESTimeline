ESDrawEnvClip : ESDrawClip {
  var <hoverIndex, <editingFirst, <originalCurve, <curveIndex;

  hoverIndex_ { |val|
    if (val != hoverIndex) {
      hoverIndex = val;
      clip.changed(\hoverIndex);
    };
  }

  prDraw { |left, top, width, height, editingMode, clipLeft, clipWidth|
    var pratio = clip.duration / width;
    var tratio = pratio.reciprocal;
    var line = clip.bus.asESDisplayString ++ "  -> " ++ clip.bus.value.asCompileString;
    var minString = clip.min.asString;
    var maxString = clip.max.asString;
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
      var thisEnv = clip.env.value;
      firstI = ((0 - left) / nratio).asInteger.clip(0, n);
      lastI = ((Window.screenBounds.width - left) / nratio).asInteger.clip(0, n);
      prevY = top + ((1 - thisEnv[clip.offset + (firstI * pratio)]) * height);
      (firstI..lastI).do { |i|
        var thisY, thisX;
        i = i * nratio;
        thisX = left + i;
        thisY = top + ((1 - thisEnv[clip.offset + (i * pratio)]) * height);
        Pen.addRect(Rect(thisX, min(prevY, thisY), nratio, max(1, abs(prevY - thisY))));
        prevY = thisY;
      };

      Pen.color = Color.gray(1, 0.8);
      Pen.fill;
    };

    if (editingMode) {
      var thisEnv = clip.envToPlay;
      var points = this.envBreakPoints(thisEnv, left, top, width, height);
      points.do { |point, i|
        Pen.addOval(Rect(point.x - 3, point.y - 3, 6, 6));
        Pen.strokeColor = Color.white;
        if (i == hoverIndex) {
          var val = clip.valueAtIndex(i);
          Pen.stringAtPoint(val.asString, point.x@(if (point.y - 20 < top) { point.y + 20 } { point.y - 20 }), Font.sansSerif(15), Color.white);
          Pen.fillColor = Color.white;
        } {
          Pen.fillColor = clip.color;
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

    ^clip.prTitle;
  }

  prMouseMove { |x, y, xDelta, yDelta, mods, left, top, width, height|
    var thisEnv = clip.envToPlay;
    var points;
    if (hoverIndex == 0) {
      clip.env = Env([thisEnv.levels[0]] ++ thisEnv.levels, [0] ++ thisEnv.times, if (thisEnv.curves.isArray) { [thisEnv.curves[0]] ++ thisEnv.curves } { thisEnv.curves });
      clip.offset = 0;
      hoverIndex = 1;
      editingFirst = true;
      thisEnv = clip.envToPlay;
    };
    points = this.envBreakPoints(thisEnv, left, top, width, height);
    if (hoverIndex.notNil) {
      var env, offset;
      // adjust breakpoint
      var prevPoint = points[max(0, hoverIndex - 1)];
      var nextPoint = if (hoverIndex < (points.size - 1)) { points[hoverIndex + 1] } { (left + width)@0 };
      var adjustedX = x.clip(prevPoint.x, nextPoint.x).clip(left, left + width);
      var adjustedY = y.clip(top, top + height);
      points[hoverIndex] = adjustedX@adjustedY;
      if (editingFirst) { points[0] = left@adjustedY; };
      #env, offset = this.envFromBreakPoints(points, left, top, width, height);
      clip.env = env;
      clip.offset = offset;
    } {
      // adjust curve if not over breakpoint and no modifiers
      if (mods == 0) {
        var curves = if (thisEnv.curves.isArray) { thisEnv.curves } { thisEnv.curves.dup(thisEnv.times.size) };
        curveIndex = curveIndex ?? this.segmentIndex(points, x@y);
        originalCurve = originalCurve ?? curves[curveIndex];
        if (originalCurve.isNumber) {
          var slope = thisEnv.levels[curveIndex + 1] - thisEnv.levels[curveIndex];
          curves[curveIndex] = originalCurve + (yDelta * 0.1 * slope.sign);
          clip.env = Env(thisEnv.levels, thisEnv.times, curves);
        };
      };
    };
  }

  prMouseDown { |x, y, mods, buttNum, clickCount, left, top, width, height|
    editingFirst = false;
    originalCurve = nil;
    curveIndex = nil;

    if (mods.isShift) {
      var pratio = clip.duration / width;
      var tratio = pratio.reciprocal;
      var thisEnv = clip.envToPlay;
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
      clip.env = Env(levels, times, curves);
      clip.offset = 0;
    };

    if (mods.isAlt and: hoverIndex.notNil) {
      var thisEnv = clip.envToPlay;
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
      clip.env = Env(levels, times, curves);
      clip.offset = 0;
    };
  }

  prHover { |x, y, hoverTime, left, top, width, height|
    var thisEnv = clip.envToPlay;
    var points = this.envBreakPoints(thisEnv, left, top, width, height);
    this.hoverIndex = this.nearestBreakPointIndex(points, (x@y));
  }

  prHoverLeave {
    this.hoverIndex = nil;
  }

  envBreakPoints { |thisEnv, left, top, width, height|
    var pratio = clip.duration / width;
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
    var pratio = clip.duration / width;
    var tratio = pratio.reciprocal;
    var curves = clip.envToPlay.curves;
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


  hasEditingMode { ^true }
}