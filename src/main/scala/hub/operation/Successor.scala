package hub.operation

/**
 * Outgoing edges from one [[Operation]] in the planning graph.
 *
 * The graph itself encodes linear flow, choice, and parallel spans.
 *
 * {{{
 * Successor.Done
 * Successor.Then(historyId)
 * Successor.All(List(chemistryId, peId))
 * Successor.OneOf(List(musicId, artId)) // list order is priority
 * }}}
 */
enum Successor:
  /** Terminal operation; no further work */
  case Done

  /** Single mandatory successor */
  case Then(next: OperationId)

  /**
   * All listed successors are required and may overlap in time.
   * Downstream nodes that depend on every member wait for the latest end.
   */
  case All(next: List[OperationId])

  /**
   * Exactly one successor is active.
   * A member with [[Operation.actual]] wins; otherwise the first list element
   * is the priority default.
   */
  case OneOf(next: List[OperationId])

end Successor
