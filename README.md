
1) Создать папку clientserver
2) Выгрузить в созданную папку:
   * данный проект
   * проект https://github.com/malinkovich/server
3) Установить Docker по ссылке:
   https://www.docker.com/products/docker-desktop/
5) Открыть терминал (консоль)
6) Перейти в проект client:
   cd /home/client
7) Выполнить команду docker-compose up
   * В случае, если команда недоступна, установить ее, выполняя команды:
      sudo curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
      sudo chmod +x /usr/local/bin/docker-compose
   * Для остановки приложения использовать команду: docker-compose down
9) Открыть любой веб-браузер
10) Перейти по ссылке http://8080/client 
