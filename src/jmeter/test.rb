require 'ruby-jmeter'
require_relative 'pages/home_page'

test do
  threads 1, name: 'Guest User' do
    home = Pages::HomePage.new(self)
    home.visit # visit as guest user
  end
  threads 1, name: 'Signed-in User' do
    home = Pages::HomePage.new(self)
    home.visit # visit as guest user
    home.sign_in # sign in
    home.visit # visit as signed-in user
  end
end.jmx(file: 'test.jmx')
