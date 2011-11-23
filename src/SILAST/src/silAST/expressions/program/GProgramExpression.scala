package silAST.expressions.program

import scala.collection.Seq
import silAST.expressions.logical.LogicalExpression
import silAST.source.SourceLocation
import silAST.expressions.terms.GTerm
import silAST.expressions.logical.GLogicalExpression

abstract class GProgramExpression[+T <: GTerm[T]]( 
		sl : SourceLocation 
	) 
	extends GLogicalExpression[T](sl) 
{
  def subExpressions(): Seq[GLogicalExpression[T]]
}