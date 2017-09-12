package com.testwithspring.master.web

import com.github.springtestdbunit.DbUnitTestExecutionListener
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DbUnitConfiguration
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader
import com.testwithspring.master.IntegrationTest
import com.testwithspring.master.IntegrationTestContext
import com.testwithspring.master.Tasks
import com.testwithspring.master.Users
import com.testwithspring.master.config.Profiles
import org.junit.experimental.categories.Category
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import spock.lang.Specification

import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.is
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = [IntegrationTestContext.class])
@WebAppConfiguration
@TestExecutionListeners(value = DbUnitTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@DatabaseSetup([
        '/com/testwithspring/master/users.xml',
        '/com/testwithspring/master/tasks.xml'
])
@DbUnitConfiguration(dataSetLoader = ReplacementDataSetLoader.class)
@Category(IntegrationTest.class)
@ActiveProfiles(Profiles.INTEGRATION_TEST)
class SearchSpec extends Specification {

    @Autowired
    WebApplicationContext webAppContext

    def mockMvc

    def setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext)
                .apply(springSecurity())
                .build()
    }

    def 'Search tasks as an anonymous user'() {

        def response

        when: 'An anonymous user tries to search tasks'
        response = search(Tasks.SEARCH_TERM_ONE_MATCH)

        then: 'Should return the HTTP status code unauthorized'
        response.andExpect(status().isUnauthorized())

        and: 'Should return an empty response'
        response.andExpect(content().string(''))
    }

    @WithUserDetails(Users.JohnDoe.EMAIL_ADDRESS)
    def 'Search tasks as a registered user'() {

        def response

        when: 'No tasks is found with the given search term'
        response = search(Tasks.SEARCH_TERM_NOT_FOUND)

        then: 'Should return the HTTP status code OK'
        response.andExpect(status().isOk())

        and: 'Should return search results as JSON'
        response.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))

        and: 'Should return zero tasks'
        response.andExpect(jsonPath('$', hasSize(0)))

        when: 'One task is found with the given search term'
        response = search(Tasks.SEARCH_TERM_ONE_MATCH)

        then: 'Should return the HTTP status code OK'
        response.andExpect(status().isOk())

        and: 'Should return search results as JSON'
        response.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))

        and: 'Should return one task'
        response.andExpect(jsonPath('$', hasSize(1)))
        
        and: 'Should return the information of the found task'
        response .andExpect(jsonPath('$[0].id', is(Tasks.WriteLesson.ID.intValue())))
                .andExpect(jsonPath('$[0].status', is(Tasks.WriteLesson.STATUS.toString())))
                .andExpect(jsonPath('$[0].title', is(Tasks.WriteLesson.TITLE)))
    }

    @WithUserDetails(Users.AnneAdmin.EMAIL_ADDRESS)
    def 'Search tasks as an admin'() {

        def response

        when: 'No tasks is found with the given search term'
        response = search(Tasks.SEARCH_TERM_NOT_FOUND)

        then: 'Should return the HTTP status code OK'
        response.andExpect(status().isOk())

        and: 'Should return search results as JSON'
        response.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))

        and: 'Should return zero tasks'
        response.andExpect(jsonPath('$', hasSize(0)))

        when: 'One task is found with the given search term'
        response = search(Tasks.SEARCH_TERM_ONE_MATCH)

        then: 'Should return the HTTP status code OK'
        response.andExpect(status().isOk())

        and: 'Should return search results as JSON'
        response.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))

        and: 'Should return one task'
        response.andExpect(jsonPath('$', hasSize(1)))

        and: 'Should return the information of the found task'
        response .andExpect(jsonPath('$[0].id', is(Tasks.WriteLesson.ID.intValue())))
                .andExpect(jsonPath('$[0].status', is(Tasks.WriteLesson.STATUS.toString())))
                .andExpect(jsonPath('$[0].title', is(Tasks.WriteLesson.TITLE)))
    }

    private ResultActions search(searchTerm) {
        return mockMvc.perform(get('/api/task/search')
                .param(WebTestConstants.RequestParameter.SEARCH_TERM, searchTerm)
        )
    }
}
