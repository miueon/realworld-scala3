package realworld.components.widgets


import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import realworld.routes.Page

def Pagination(
  currentPage: Page,
  count: Int,
  itermsPerPage: Int,
  // onPageChange: 
) = 
  navTag()