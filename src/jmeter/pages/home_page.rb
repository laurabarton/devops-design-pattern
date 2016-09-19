require 'ruby-jmeter'
require_relative 'base_page'

module Pages
  class HomePage
    include Pages::BasePage

    def initialize(dsl)
      @dsl = dsl
    end

    def visit
      get name: 'home', url: '/'
    end

    def sign_in
      post name: 'sign_in', url: '/signin'
    end
  end
end
