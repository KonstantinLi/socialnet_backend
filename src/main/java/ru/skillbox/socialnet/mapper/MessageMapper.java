package ru.skillbox.socialnet.mapper;

import java.time.ZoneId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import ru.skillbox.socialnet.dto.response.MessageRs;
import ru.skillbox.socialnet.dto.response.PersonRs;
import ru.skillbox.socialnet.dto.websocket.MessageWs;
import ru.skillbox.socialnet.entity.dialogrelated.Message;
import ru.skillbox.socialnet.entity.personrelated.Person;

@Mapper(componentModel = "spring", imports = ZoneId.class)
public interface MessageMapper {

  MessageMapper INSTANCE = Mappers.getMapper(MessageMapper.class);

  @Mapping(target = "authorId", source = "author.id")
  @Mapping(target = "recipientId", source = "recipient.id")
  MessageRs messageToMessageRs(Message message);

  @Mapping(target = "authorId", source = "message.author.id")
  @Mapping(target = "recipientId", source = "message.recipient.id")
  @Mapping(target = "time", expression = "java( message.getTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() )")
  MessageWs messageToMessageWs(Message message, String token);

  default PersonRs personToPersonRs(Person person) {
    return PersonMapper.INSTANCE.personToPersonRs(person);
  }

}