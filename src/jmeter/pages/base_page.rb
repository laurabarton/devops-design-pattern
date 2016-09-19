require 'ruby-jmeter'

module Pages
  module BasePage

    def method_missing method, *args, &block
      @dsl.__send__ method, *args, &block
    end
  end
end
