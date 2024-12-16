package realworld.components.widgets

import com.raquo.laminar.api.L.*

import scala.scalajs.js.Math

def Pagination(
  currentPage: Signal[Int],
  count: Signal[Int],
  itemsPerPage: Int,
  onPageChange: Observer[Int]
) =
  navTag(
    ul(
      cls := "pagination",
      children <-- count.map { count =>
        val pages = Math.ceil(count / itemsPerPage).toInt
        if pages > 1 then
          val pagesList = (1 to pages + 1).toList
          pagesList.map { page =>
            li(
              cls <-- currentPage.map(cur =>
                if cur == page then "page-item active" else "page-item"
              ),
              a(
                cls("page-link"),
                href := "#",
                onClick.mapTo(page) --> onPageChange,
                page.toString
              )
            )
          }
        else List()
        end if
      }
    )
  )
