ESTimelineClipEditView : ESClipEditView {
  *new { |clip, timeline|
    var win = ESTimelineWindow("Timeline Clip", timeline: clip.timeline);
    win.bounds = win.bounds.top_(win.bounds.top - 50);
  }
}