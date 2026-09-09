package com.rockthejvm.reviewboard.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.*
import com.rockthejvm.reviewboard.core.ZioLaminar.*
import com.rockthejvm.reviewboard.domain.data.CompanyFilter
import com.rockthejvm.reviewboard.domain.data.CompanyFilter2
import org.scalajs.dom
import zio.*

class FilterPanel {
  case class CheckValueEvent(groupName: String, value: String, checked: Boolean)

  private val GroupLocations  = "Locations"
  private val GroupCountries  = "Countries"
  private val GroupIndustries = "Industries"
  private val GroupTags       = "Tags"

  private val possibleFilter     = EventBus[CompanyFilter]()
  private val checkEvents        = EventBus[CheckValueEvent]()
  private val applyFiltersClicks = EventBus[Unit]()
  private val enableApplyFilters =
    applyFiltersClicks.events.mapTo(false).mergeWith(checkEvents.events.mapTo(true))

  private val state: Signal[CompanyFilter] = checkEvents.events
    .scanLeft(Map[String, Set[String]]()) { (currentMap, event) =>
      event match {
        case CheckValueEvent(groupName, value, checked) =>
          if (checked) currentMap + (groupName -> (currentMap.getOrElse(groupName, Set()) + value))
          else currentMap + (groupName         -> (currentMap.getOrElse(groupName, Set()) - value))
      }
    }
    .map { checkMap =>
      CompanyFilter(
        locations = checkMap.getOrElse(GroupLocations, Set()).toList,
        countries = checkMap.getOrElse(GroupCountries, Set()).toList,
        industries = checkMap.getOrElse(GroupIndustries, Set()).toList,
        tags = checkMap.getOrElse(GroupTags, Set()).toList
      )
    }
  private val state2: Signal[CompanyFilter2] = checkEvents.events
    .scanLeft(CompanyFilter2(Map())) { (currentState, event) =>
      val updatedChecks = currentState.checks.updatedWith(event.groupName) {
        case Some(groupMap) => Some(groupMap.updated(event.value, event.checked))
        case None           => Some(Map(event.value -> event.checked))
      }
      CompanyFilter2(updatedChecks)
    }

  // Informs CompanyPage to rerender
  val triggerFilters: EventStream[CompanyFilter] =
    applyFiltersClicks.events.withCurrentValueOf(state)

  def apply() =
    div(
      onMountCallback(_ => useBackend(_.companies.allFiltersEndpoint(())).emitTo(possibleFilter)),
      cls    := "accordion accordion-flush",
      idAttr := "accordionFlushExample",
      div(
        cls := "accordion-item",
        h2(
          cls    := "accordion-header",
          idAttr := "flush-headingOne",
          button(
            cls                                         := "accordion-button",
            idAttr                                      := "accordion-search-filter",
            `type`                                      := "button",
            htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
            htmlAttr("data-bs-target", StringAsIsCodec) := "#flush-collapseOne",
            htmlAttr("aria-expanded", StringAsIsCodec)  := "true",
            htmlAttr("aria-controls", StringAsIsCodec)  := "flush-collapseOne",
            div(
              cls := "jvm-recent-companies-accordion-body-heading",
              h3(
                span("Search"),
                " Filters"
              )
            )
          )
        ),
        div(
          cls                                          := "accordion-collapse collapse show",
          idAttr                                       := "flush-collapseOne",
          htmlAttr("aria-labelledby", StringAsIsCodec) := "flush-headingOne",
          htmlAttr("data-bs-parent", StringAsIsCodec)  := "#accordionFlushExample",
          div(
            cls := "accordion-body p-0",
            renderFilterOptions(GroupLocations, _.locations),
            renderFilterOptions(GroupCountries, _.countries),
            renderFilterOptions(GroupIndustries, _.industries),
            renderFilterOptions(GroupTags, _.tags),
            renderApplyFilters
          )
        )
      )
    )

  def renderFilterOptions(groupName: String, optionsFun: CompanyFilter => List[String]) =
    div(
      cls := "accordion-item",
      h2(
        cls    := "accordion-header",
        idAttr := s"heading$groupName",
        button(
          cls                                         := "accordion-button collapsed",
          `type`                                      := "button",
          htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
          htmlAttr("data-bs-target", StringAsIsCodec) := s"#collapse$groupName",
          htmlAttr("aria-expanded", StringAsIsCodec)  := "false",
          htmlAttr("aria-controls", StringAsIsCodec)  := s"collapse$groupName",
          groupName
        )
      ),
      div(
        cls                                          := "accordion-collapse collapse",
        idAttr                                       := s"collapse$groupName",
        htmlAttr("aria-labelledby", StringAsIsCodec) := "headingOne",
        htmlAttr("data-bs-parent", StringAsIsCodec)  := "#accordionExample",
        div(
          cls := "accordion-body",
          div(
            cls := "mb-3",
            children <-- possibleFilter.events.toSignal(CompanyFilter.empty).map { filter =>
              optionsFun(filter).map(value => renderFilterCheckbox(groupName, value))
            }
          )
        )
      )
    )

  private def renderApplyFilters =
    div(
      cls := "jvm-accordion-search-btn",
      button(
        disabled <-- enableApplyFilters.toSignal(false).map(v => !v),
        onClick.mapTo(()) --> applyFiltersClicks,
        cls    := "btn btn-primary",
        `type` := "button",
        "Apply Filters"
      )
    )

  private def renderFilterCheckbox(groupName: String, value: String) =
    div(
      cls := "form-check",
      label(
        cls   := "form-check-label",
        forId := s"filter-$groupName-$value",
        value
      ),
      input(
        cls    := "form-check-input",
        `type` := "checkbox",
        idAttr := s"filter-$groupName-$value",
        onChange.mapToChecked.map(CheckValueEvent(groupName, value, _)) --> checkEvents
      )
    )
}
