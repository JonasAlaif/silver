
package viper.silver.plugin.termination

import viper.silver.ast.utility.Statements.EmptyStmt
import viper.silver.ast._

trait RewriteFunctionBody[C <: Context] extends TerminationCheck {

  /**
    * Transforms an expression (e.g. function body) into a statement.
    * Parts of the expressions which stay expressions (e.g. the condition in a if clause)
    * are added in front as statements.
    * Expressions which cannot be transformed to statements (e.g. literals) are replaced
    * by the transfromExp.
    *
    * @return a statement representing the expression
    */
  def transform: PartialFunction[(Exp, C), Stmt] = {
    case (pap: PredicateAccessPredicate, _) =>
      EmptyStmt
    case (callee: FuncApp, _) =>
      EmptyStmt
    case (CondExp(cond, thn, els), c) =>
      val condStmt = transform(cond, c)
      val thnStmt = transform(thn, c)
      val elsStmt = transform(els, c)
      val ifStmt = If(transformExp(cond, c), Seqn(Seq(thnStmt), Nil)(), Seqn(Seq(elsStmt), Nil)())()
      Seqn(Seq(condStmt, ifStmt), Nil)()
    case (Unfolding(acc, unfBody), c) =>
      // TODO: needed? val accExp = transformExp(acc, c)
      val unfold = Unfold(acc)()
      val access = transform(acc, c)
      val unfoldBody = transform(unfBody, c)
      val fold = Fold(acc)()
      // TODO: shouldn't access be before unfold?
      Seqn(Seq(unfold, access, unfoldBody, fold), Nil)()
    case (b: BinExp, c) =>
      val left = transform(b.left, c)
      val right = transform(b.right, c)
      // Short circuit evaluation
      b match {
        case _: Or =>
          Seqn(Seq(left,
            If(Not(b.left)(b.pos), Seqn(Seq(right), Nil)(b.pos), EmptyStmt)(b.pos)),
            Nil)(b.pos)
        case _: And =>
          Seqn(Seq(left,
            If(b.left, Seqn(Seq(right), Nil)(b.pos), EmptyStmt)(b.pos)),
            Nil)(b.pos)
        case _: Implies =>
          Seqn(Seq(left,
            If(b.left, Seqn(Seq(right), Nil)(b.pos), EmptyStmt)(b.pos)),
            Nil)(b.pos)
        case _ =>
          Seqn(Seq(left, right), Nil)(b.pos)
      }
    case (sq: SeqExp, c) => sq match {
      case ExplicitSeq(elems) =>
        Seqn(elems.map(transform(_, c)), Nil)(sq.pos)
      case RangeSeq(low, high) =>
        Seqn(Seq(transform(low, c),
          transform(high, c)), Nil)(sq.pos)
      case SeqAppend(left, right) =>
        Seqn(Seq(transform(left, c),
          transform(right, c)), Nil)(sq.pos)
      case SeqIndex(s, idx) =>
        Seqn(Seq(transform(s, c),
          transform(idx, c)), Nil)(sq.pos)
      case SeqTake(s, n) =>
        Seqn(Seq(transform(s, c),
          transform(n, c)), Nil)(sq.pos)
      case SeqDrop(s, n) =>
        Seqn(Seq(transform(s, c),
          transform(n, c)), Nil)(sq.pos)
      case SeqContains(elem, s) =>
        Seqn(Seq(transform(elem, c),
          transform(s, c)), Nil)(sq.pos)
      case SeqUpdate(s, idx, elem) =>
        Seqn(Seq(transform(s, c),
          transform(idx, c),
          transform(elem, c)), Nil)(sq.pos)
      case SeqLength(s) =>
        Seqn(Seq(transform(s, c)), Nil)(sq.pos)
      case _: Exp => EmptyStmt
    }
    case (st: ExplicitSet, c) =>
      Seqn(st.elems.map(transform(_, c)), Nil)(st.pos)
    case (mst: ExplicitMultiset, c) =>
      Seqn(mst.elems.map(transform(_, c)), Nil)(mst.pos)
    case (u: UnExp, c) => transform(u.exp, c)
    case _ => EmptyStmt
  }

  def transformExp(exp: Exp, context: C): Exp
}

trait Context